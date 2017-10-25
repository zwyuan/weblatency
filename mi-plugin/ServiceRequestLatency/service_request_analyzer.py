'''
service_request_analyzer.py

It analyses newly generated cellular event log file,
log and decode them, then save the log to external storage.

Author  : Zengwen Yuan
Version : 1.0  Init from old NetLogger code
'''

import os
import time
import shutil
import urllib
import urllib2
import logging
import itertools
import mimetools
import mimetypes
import threading
import subprocess
from datetime import datetime

from mobile_insight.analyzer import Analyzer
from service import mi2app_utils as util

import jnius
from jnius import autoclass, cast
from android.broadcast import BroadcastReceiver


ANDROID_SHELL = "/system/bin/sh"

__all__ = ['ServiceRequestAnalyzer', 'MultiPartForm']


msgs = [    'service request message (12)',
            'rrcconnectionrequest_element',
            'rrcconnectionsetup_element',
            'rrcconnectionsetupcomplete_element',
            'securitymodecommand_element',
            'securitymodecomplete_element',
            'rrcconnectionreconfiguration_element',
            'rrcconnectionreconfigurationcomplete_element',
            ]

states = [  'null',
            'ServiceRequestBegin',
            'RrcConnectionSetupRequest',
            'RrcConnectionSetupAccept',
            'RrcConnectionSetupComplete',
            'RrcSecurityModeCommand',
            'RrcSecurityModeCommandComplete',
            'RrcConnectionReconfiguration',
            'ServiceRequestComplete',
            ]

transitions = [
    { 'trigger': msgs[0], 'source': states[0], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[1], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[2], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[3], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[4], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[5], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[6], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[7], 'dest': states[1] },
    { 'trigger': msgs[0], 'source': states[8], 'dest': states[1] },
    { 'trigger': msgs[1], 'source': states[1], 'dest': states[2] },
    { 'trigger': msgs[2], 'source': states[2], 'dest': states[3] },
    { 'trigger': msgs[3], 'source': states[3], 'dest': states[4] },
    { 'trigger': msgs[4], 'source': states[4], 'dest': states[5] },
    { 'trigger': msgs[5], 'source': states[5], 'dest': states[6] },
    { 'trigger': msgs[6], 'source': states[6], 'dest': states[7] },
    { 'trigger': msgs[7], 'source': states[7], 'dest': states[8] }
]

def upload_log(filename):
    succeed = False
    form = MultiPartForm()
    form.add_field('file[]', filename)
    form.add_file('file', filename)
    request = urllib2.Request(
        'http://metro.cs.ucla.edu/mobile_insight/upload_file.php')
    request.add_header("Connection", "Keep-Alive")
    request.add_header("ENCTYPE", "multipart/form-data")
    request.add_header('Content-Type', form.get_content_type())
    body = str(form)
    request.add_data(body)

    try:
        response = urllib2.urlopen(request, timeout=3).read()
        if response.startswith("TW9iaWxlSW5zaWdodA==FILE_SUCC") \
                or response.startswith("TW9iaWxlSW5zaWdodA==FILE_EXST"):
            succeed = True
    except urllib2.URLError as e:
        pass
    except socket.timeout as e:
        pass

    if succeed is True:
        try:
            file_base_name = os.path.basename(filename)
            uploaded_file = os.path.join(
                util.get_mobileinsight_log_uploaded_path(), file_base_name)
            # TODO: print to screen
            # print "debug 58, file uploaded has been renamed to %s" % uploaded_file
            # shutil.copyfile(filename, uploaded_file)
            util.run_shell_cmd("cp %s %s" % (filename, uploaded_file))
            os.remove(filename)
        finally:
            util.detach_thread()


class MultiPartForm(object):

    def __init__(self):
        self.form_fields = []
        self.files = []
        self.boundary = mimetools.choose_boundary()
        return

    def get_content_type(self):
        return 'multipart/form-data; boundary=%s' % self.boundary

    def add_field(self, name, value):
        self.form_fields.append((name, value))
        return

    def add_file(self, fieldname, filename, mimetype=None):
        fupload = open(filename, 'rb')
        body = fupload.read()
        fupload.close()
        if mimetype is None:
            mimetype = mimetypes.guess_type(
                filename)[0] or 'application/octet-stream'
        self.files.append((fieldname, filename, mimetype, body))
        return

    def __str__(self):
        parts = []
        part_boundary = '--' + self.boundary
        parts.extend([part_boundary,
                      'Content-Disposition: form-data; name="%s"; filename="%s"' % (name,
                                                                                    value)] for name,
                     value in self.form_fields)

        parts.extend(
            [
                part_boundary,
                'Content-Disposition: file; name="%s"; filename="%s"' %
                (field_name,
                 filename),
                'Content-Type: %s' %
                content_type,
                '',
                body,
            ] for field_name,
            filename,
            content_type,
            body in self.files)

        flattened = list(itertools.chain(*parts))
        flattened.append('--' + self.boundary + '--')
        flattened.append('')
        return '\r\n'.join(flattened)


class ServiceRequestAnalyzer(Analyzer):
    """
    An analyzer for cellular events logging and decoding
    """

    def __init__(self, config):
        Analyzer.__init__(self)

        self.__log_dir = util.get_mobileinsight_log_path()
        self.__dec_log_dir = util.get_mobileinsight_log_decoded_path()
        self.__orig_file = ""
        self.__raw_msg = {}
        self.__raw_msg_key = ""
        self.__msg_cnt = 0
        self.__dec_msg = []
        self.__is_wifi_enabled = False
        self.state = states[0]
        self.sr_round = 0
        self.round = 0
        self.sr_record = []
        self.record = {'round': 0, 'total': 0, 'msg':[],}
        self.__sr_log_path = "%s/sr/sr_latency_%s_%s_%s.txt" % \
            (util.get_mobileinsight_path(), datetime.now().strftime('%Y%m%d_%H%M%S'), util.get_phone_info(), util.get_operator_info())
        
        self.__MILabPluginName = 'SRLatency'
        self.__own_log_uploaded_dir = os.path.join(
                util.get_mobileinsight_plugin_path(),
                self.__MILabPluginName,
                "log", "uploaded")
        if not os.path.exists(self.__own_log_uploaded_dir):
            os.makedirs(self.__own_log_uploaded_dir)

        self.__task = "Unknown"
        if 'task' in config:
            self.__task = config['task']

        self.__own_log_task_dir = os.path.join(
                util.get_mobileinsight_plugin_path(),
                self.__MILabPluginName,
                "log", self.__task)
        if not os.path.exists(self.__own_log_task_dir):
            os.makedirs(self.__own_log_task_dir)

        try:
            if config['is_use_wifi'] == '1':
                self.__is_use_wifi = True
            else:
                self.__is_use_wifi = False
        except BaseException:
            self.__is_use_wifi = False
        # try:
        #     if config['is_dec_log'] == '1':
        #         self.__is_dec_log = True
        #         self.__dec_log_name = "diag_log_" + \
        #             datetime.datetime.now().strftime('%Y%m%d_%H%M%S') + ".txt"
        #         self.__dec_log_path = os.path.join(
        #             self.__dec_log_dir, self.__dec_log_name)
        #     else:
        #         self.__is_dec_log = False
        # except BaseException:
        #     self.__is_dec_log = False
        try:
            self.__dec_log_type = config['log_type']
        except BaseException:
            self.__dec_log_type = ""

        if not os.path.exists(self.__log_dir):
            os.makedirs(self.__log_dir)
        if not os.path.exists(self.__dec_log_dir):
            os.makedirs(self.__dec_log_dir)

        self.add_source_callback(self._logger_filter)

        # Add this code at the end of onCreate()
        self.br = BroadcastReceiver(self.on_broadcast,
                actions=['MobileInsight.Main.StopService'])
        self.br.start()

    def on_broadcast(self, context, intent):
        '''
        This plugin is going to be stopped, finish closure work
        '''
        print "LoggingAnalyzer are going to be stopped"
        # self._check_orphan_log()
        IntentClass = autoclass("android.content.Intent")
        intent = IntentClass()
        action = 'MobileInsight.Plugin.StopServiceAck'
        intent.setAction(action)
        try:
            util.pyService.sendBroadcast(intent)
        except Exception as e:
            import traceback
            self.log_error(str(traceback.format_exc()))
            
    def __del__(self):
        self.log_info("__del__ is called")


    def trigger(self, msg_trigger):
        for tr in transitions:
            if msg_trigger == tr['trigger'] and self.state == tr['source']:
                # self.log_warning("[local time = %s] Source state = %s" % (datetime.now(), tr['source']))
                ts = time.time()
                self.log_warning("[epoch = %s][local = %s] Source state = %s" % (ts, datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S'), tr['source']))
                self.state = tr['dest']
                if tr == transitions[-1]:
                    self.sr_round += 1
                return True
        return False


    def _logger_filter(self, msg):
        """
        Callback to process new generated logs.

        :param msg: the message from trace collector.
        :type msg: Event
        """

        if msg.type_id == "LTE_NAS_EMM_OTA_Outgoing_Packet":
            log_item = msg.data.decode()

            if msgs[0] in log_item['Msg'].lower():
                # self.log_warning("Service request")
                if self.trigger(msgs[0]):
                    self.round += 1
                    self.start_ts = log_item['timestamp']
                    self.log_warning("Now state == %s" % self.state)
                    self.record['round'] = self.sr_round
                    self.record['msg'].append("[%s; %s]" % (log_item['timestamp'], self.state))

        if msg.type_id == "LTE_RRC_OTA_Packet":
            log_item = msg.data.decode()

            for i in range(1, 8):
                if msgs[i] in log_item['Msg'].lower():
                    if self.trigger(msgs[i]):
                        # convert the rrc message timestamp to epoch
                        epoch = time.mktime(time.strptime(log_item['timestamp'].strftime("%Y-%m-%d %H:%M:%S"), "%Y-%m-%d %H:%M:%S"))
                        # ts.strftime('%Y%m%d_%H%M%S')
                        # time.strptime("2008-09-17 14:04:00", "%Y-%m-%d %H:%M:%S")
                        # self.log_warning("%s" % epoch)
                        # epoch = (datetime.datetime.utcfromtimestamp(ts) - datetime.datetime(1970,1,1)).total_seconds()
                        self.log_warning("[epoch = %s][log_utc = %s] Incoming RRC msg[%d] = %s" % (epoch, str(log_item['timestamp']), i, msgs[i]))
                        # self.log_warning("Now state == %s" % self.state)
                        # self.record['round'] = self.sr_round
                        self.record['msg'].append("[%s; %s]" % (log_item['timestamp'], self.state))

                        if msgs[i] == msgs[-1]:
                            new_ts = (log_item['timestamp'] - self.start_ts).microseconds / 1000
                            self.log_warning("Total SR time for round %d = %s ms" % (self.sr_round, new_ts))
                            self.record['msg'].append("[%s; %s; %s]" % (epoch, log_item['timestamp'], self.state))
                            self.record['total'] = "%d" % new_ts

                            try:
                                with open(self.__sr_log_path, 'a') as f:
                                    f.writelines(str(self.record))
                                    f.writelines('\n')
                            except BaseException:
                                pass
                            self.sr_record.append(self.record)

                            # self.log_warning("%s" % (self.record))
                            # self.log_warning("%s" % (self.sr_record))
                            self.record = {'round':"", 'msg':[], 'total':""}

        # when a new log comes, save it to external storage and upload
        if msg.type_id.find("new_diag_log") != -1:
            self.__log_timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            self.__orig_file = msg.data.decode().get("filename")

            # FIXME (Zengwen): the change access command is a walkaround
            # solution
            util.run_shell_cmd("chmod 644 %s" % self.__orig_file)

            self._save_log()
            self.__is_wifi_enabled = util.get_wifi_status()

            if self.__is_use_wifi is True and self.__is_wifi_enabled is True:
                try:
                    for f in os.listdir(self.__log_dir):
                        if f.endswith(".mi2log"):
                            orphan_file = os.path.join(self.__log_dir, f)
                            t = threading.Thread(
                                target=upload_log, args=(orphan_file, ))
                            t.start()
                except Exception as e:
                    pass
            else:
                # use cellular data to upload. Skip for now.
                pass

    def _save_log(self):
        orig_base_name = os.path.basename(self.__orig_file)
        orig_dir_name = os.path.dirname(self.__orig_file)
        milog_base_name = "diag_log_%s_%s_%s.mi2log" % (
            self.__log_timestamp, util.get_phone_info(), util.get_operator_info())
        # milog_abs_name = os.path.join(self.__log_dir, milog_base_name)
        milog_abs_name  = os.path.join(self.__own_log_task_dir, milog_base_name)
        if os.path.isfile(self.__orig_file):
            shutil.copyfile(self.__orig_file, milog_abs_name)
            os.remove(self.__orig_file)

        return milog_abs_name
