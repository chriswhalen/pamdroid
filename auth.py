#!/usr/bin/env python

import configparser

import pyotp

import requests


config_path = '/etc/pamdroid.conf'

config = configparser.ConfigParser()
config.read_file(open(config_path, 'r'))
server_url = config.get('SERVER', 'url')
secret = config.get('SECRET', 'secret')

totp = pyotp.TOTP(secret)

try:
    r = requests.get(server_url + '/identity', timeout=2)
    content = r.json()

except Exception:
    print('Unable to reach phone')
    exit(2)

if content['identity'] != 'Pamdroid':
    print('Unable to reach phone')
    exit(2)

try:
    r = requests.get(server_url + '/token', timeout=15)
    content = r.json()

except Exception:
    print('Unable to reach phone')
    exit(2)

if totp.verify(content['token']):
    print('Authenticated')
    exit(0)

else:
    print('Authentication failed')
    exit(1)
