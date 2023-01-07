#!/usr/bin/env python

"""A python script to assign numbers to accounts"""

from __future__ import print_function
import argparse
import json
import csv
import re
from time import sleep
import requests

METRICS = {
    'FIXED_NUMBERS': 0,
    'ANALYZED_NUMBERS': 0,
    'TOTAL_NUMBERS': 0,
    'NUMBER_ASSIGNMENT_ERROR': 0,
    'NUMBER_NOT_FOUND': 0
}

BASE_ENDPOINT = '/v1/numbers/'
ASSIGNMENT_PATH = '/assignment'


def load_file(file_path):
    """ Returns a list of 'number' and 'accountId' data pair from a file """
    rows = []
    with open(file_path, 'r') as file:
        for row in csv.reader(file, delimiter=','):
            rows.append({
                'number': row[0],
                'accountId': row[1]
            })
    return rows


def get_regex(number):
    """ Return exact regex of number """
    escaped = re.escape(number)
    regex = '^' + escaped + '$'

    return regex


def get_number_id_unassigned(base_url, number):
    """ Return the number id """
    params = {'matching': get_regex(number), 'assigned': False}
    response = requests.get(base_url + BASE_ENDPOINT, params=params)

    number_id = None
    if response.status_code == 200:
        numbers = response.json()['numbers']
        if numbers:
            number_id = numbers[0]['id']

    return number_id


def assign_number_id_to_account(base_url, vendor, account, number_id, note, number):
    """ Assign number id to account """
    headers = {'Content-Type': 'application/json'}
    assignment = {'vendorId': vendor,
                  'accountId': account,
                  'metadata': {'note': note}}

    response = requests.post(base_url + BASE_ENDPOINT + number_id + ASSIGNMENT_PATH,
                             data=json.dumps(assignment),
                             headers=headers)

    if response.status_code == 201:
        METRICS['FIXED_NUMBERS'] += 1
    else:
        METRICS['NUMBER_ASSIGNMENT_ERROR'] += 1
        print('Error on assignment: ', number, '-', account, ', Status code: ', response.status_code)


def display_metrics():
    """ Displays some metrics """
    print('Fixed Numbers: ', METRICS['FIXED_NUMBERS'],
          ', Analyzed Numbers: ', METRICS['ANALYZED_NUMBERS'],
          ', Total Numbers', METRICS['TOTAL_NUMBERS'],
          ', Numbers Left: ', METRICS['TOTAL_NUMBERS'] - METRICS['ANALYZED_NUMBERS'] - METRICS['NUMBER_NOT_FOUND'],
          ', Error Fixing: ', METRICS['NUMBER_ASSIGNMENT_ERROR'],
          ', Numbers Missing: ', METRICS['NUMBER_NOT_FOUND'])


def main():
    """ Do some work """
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--base-url', help="The Base URL of Numbers Service", required=True)
    parser.add_argument('--note', help="Note SUPPORT ticket number and details.", required=True)
    parser.add_argument('--vendor', help="The account vendor (default = MessageMedia)", default='MessageMedia')
    parser.add_argument('--file', help="The csv with numbers and accounts to process", required=True)
    parser.add_argument('--sleep', help="Wait in milliseconds between batch", default=100)
    parser.add_argument('--size', help="The amount of analyzed numbers before sleep", default=100)
    parser.add_argument('--display', help="Display metrics every this amount" +
                                          " of analyzed numbers", default=1000)
    args = parser.parse_args()

    try:
        # Read numbers + accounts file
        number_accounts = load_file(args.file)

        METRICS['TOTAL_NUMBERS'] = len(number_accounts)

        # Iterate each number for processing
        for number_account in number_accounts:
            number_id = get_number_id_unassigned(args.base_url, number_account['number'])

            # Assign number to account if number is unassigned
            if number_id is not None:
                assign_number_id_to_account(args.base_url, args.vendor, number_account['accountId'],
                                            number_id, args.note, number_account['number'])

                METRICS['ANALYZED_NUMBERS'] += 1
                if METRICS['ANALYZED_NUMBERS'] % int(args.display) == 0:
                    display_metrics()
                if METRICS['ANALYZED_NUMBERS'] % int(args.size) == 0:
                    sleep(int(args.sleep) / 1000)  # wait some time to not disturb ams database

            else:
                print("Error: ", number_account['number'], " is not available.")
                METRICS["NUMBER_NOT_FOUND"] += 1

        display_metrics()

    except IOError:
        print("Error: File does not exist")

    print('Process finished')


if __name__ == '__main__':
    main()
