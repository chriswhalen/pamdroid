#!/usr/bin/env python

import argparse
import base64
import configparser
import os
import sys

import pyotp

import requests

args = argparse.ArgumentParser(
    description='Authenticate from an Android fingerprint sensor.')

args.add_argument('-c', '--config', dest='path', action='store',
                  default='/etc/pamdroid.conf', help='path to pamdroid.conf')

args.add_argument('-i', '--init', dest='init', action='store_true',
                  help='initialize your fingerprint')

args.add_argument('-v', '--version', action='version',
                  version='pamdroid 1.0.0')


arguments = args.parse_args()

config = configparser.ConfigParser()
config.read_file(open(arguments.path, 'r'))

if arguments.init:

    while True:

        server_url = input('Device address: ')
        server_url = server_url.lower()

        if ':' not in server_url:

            server_port = input('Port (19200): ')

            if server_port == '':
                server_port = '19200'

            server_url = server_url + ':' + server_port

        if (server_url[:4] != 'http'):

            server_url = 'http://' + server_url

        try:
            r = requests.get(server_url + '/identity')
            content = r.json()

            if content['identity'] == 'Pamdroid':
                break

            else:
                print('Invalid address')

        except Exception:
            print('Invalid address')

    config.add_section('SERVER')
    config.set('SERVER', 'url', server_url)

    while True:

        success = True

        key = base64.b32encode(os.urandom(40)).decode()
        params = {'secret': key}

        print('Continue on your device')

        try:
            r = requests.post(server_url + '/store', params=params, timeout=15)
            content = r.json()

        except Exception:

            print('Verification failed')
            success = False

        if content['status'] == 'success':

            config.add_section('SECRET')
            config.set('SECRET', 'secret', key)
            break

        else:
            print('Verification failed')
            success = False

        if not success:

            retry = input('Retry? (Y/n) ')
            if retry == 'n':
                sys.exit()

    config.write(open(arguments.path, 'w'))
    os.chmod(arguments.config, 0o600)

    print('Configuration written to %s' % arguments.path)
    exit(0)


server_url = config.get('SERVER', 'url')
secret = config.get('SECRET', 'secret')

totp = pyotp.TOTP(secret)

try:
    r = requests.get(server_url + '/identity', timeout=2)
    content = r.json()

except Exception:
    print('Device unavailable')
    exit(2)

if content['identity'] != 'Pamdroid':
    print('Device not recognized')
    exit(3)

try:
    r = requests.get(server_url + '/token', timeout=15)
    content = r.json()

except Exception:
    print('Device unavailable')
    exit(2)

if totp.verify(content['token']):
    print('Authenticated')
    exit(0)

else:
    print('Authentication failed')
    exit(1)
