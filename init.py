#!/usr/bin/env python

import base64
import configparser
import os
import sys

import requests


def generate_key():
    return base64.b32encode(os.urandom(40)).decode()


config_path = '/etc/pamdroid.conf'

# Create config
config = configparser.ConfigParser()

# Ask input server
while True:
    server_url = input('Address: ')

    # Make lower
    server_url = server_url.lower()

    # check if port exist
    if ':' not in server_url:
        server_port = input('Port (19200): ')

        if server_port == '':
            server_port = '19200'

        server_url = server_url + ':' + server_port

    # Add http
    if (server_url[:4] != 'http'):
        server_url = 'http://' + server_url

    # Check
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

# setup secret
while True:
    success = True

    secretKey = generate_key()
    queryParam = {'secret': secretKey}

    print('Continue on phone')

    try:
        r = requests.post(server_url + '/store',
                          params=queryParam, timeout=30)
        content = r.json()

    except Exception:
        print('Verification failed')
        success = False

    if content['status'] == 'success':
        config.add_section('SECRET')
        config.set('SECRET', 'secret', secretKey)
        break

    else:
        print('Verification failed')
        success = False

    if not success:
        retry = input('Retry? (Y/n) ')
        if retry == 'n':
            sys.exit()

# write config
config.write(open(config_path, 'w'))
os.chmod(config_path, 0o600)

print('Configuration written to %s' % config_path)
