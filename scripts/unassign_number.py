#!/usr/bin/env python

"""A python script to unassign numbers"""

from __future__ import print_function
import argparse
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
    ''' Returns a list of data from a file '''
    rows = []
    with open(file_path, 'r') as file:
        for row in csv.reader(file):
            rows.extend(row)
    return rows


def get_regex(number):
    """ Return exact regex of number """
    escaped = re.escape(number)
    regex = '^' + escaped + '$'

    return regex


def get_number_id_assigned(base_url, number):
    """ Return the number id """
    params = {'matching': get_regex(number), 'assigned': True}
    response = requests.get(base_url + BASE_ENDPOINT, params=params)

    number_id = None
    if response.status_code == 200:
        numbers = response.json()['numbers']
        if numbers:
            number_id = numbers[0]['id']

    return number_id


def unassign_number_id(base_url, number_id, number):
    """ Unassign number id"""
    response = requests.delete(base_url + BASE_ENDPOINT + number_id + ASSIGNMENT_PATH)

    if response.status_code == 204:
        METRICS['FIXED_NUMBERS'] += 1
    else:
        METRICS['NUMBER_ASSIGNMENT_ERROR'] += 1
        print('Error unassigning number: ', number, ', Status code: ', response.status_code)


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
    parser.add_argument('--file', help="The csv with numbers and accounts to process", required=True)
    parser.add_argument('--sleep', help="Wait in milliseconds between batch", default=100)
    parser.add_argument('--size', help="The amount of analyzed numbers before sleep", default=100)
    parser.add_argument('--display', help="Display metrics every this amount" +
                                          " of analyzed numbers", default=1000)
    args = parser.parse_args()

    try:
        # Read numbers file
        numbers = load_file(args.file)

        METRICS['TOTAL_NUMBERS'] = len(numbers)

        # Iterate each number for processing
        for number in numbers:
            number_id = get_number_id_assigned(args.base_url, number)

            # Unassign number to account if number is assigned
            if number_id is not None:
                unassign_number_id(args.base_url, number_id, number)

                METRICS['ANALYZED_NUMBERS'] += 1
                if METRICS['ANALYZED_NUMBERS'] % int(args.display) == 0:
                    display_metrics()
                if METRICS['ANALYZED_NUMBERS'] % int(args.size) == 0:
                    sleep(int(args.sleep) / 1000)  # wait some time to not disturb ams database

            else:
                print("Info: ", number, " is not assigned.")
                METRICS["NUMBER_NOT_FOUND"] += 1

        display_metrics()

    except IOError:
        print("Error: File does not exist")

    print('Process finished')


if __name__ == '__main__':
    main()
