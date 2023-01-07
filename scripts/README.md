# Scripts

## Python Virtual Environment

Before running any of these scripts, you need to setup Python virtual environment (`venv`) and install the required depedencies from the `requirements.txt` file.

1. `python -m venv venv` (_creates new venv_)
2. `. venv/bin/activate` (_activates it_)
3. `pip install -r requirements.txt` (_installs required dependencies_)

Now you're ready to run the scripts.

## Assign Numbers to Accounts script

This script assigns numbers to accounts from file.

### Usage

    $ python assign_number_to_account.py -h
    usage: assign_number_to_account.py [-h] --base-url BASE_URL --note NOTE
                                          [--vendor VENDOR] --file FILE
                                          [--sleep SLEEP] [--size SIZE]
                                          [--display DISPLAY]
    
    A python script to assign numbers to account
    
    optional arguments:
      -h, --help           show this help message and exit
      --base-url BASE_URL  The Base URL of Numbers Service
      --note NOTE          Note SUPPORT ticket number and details.
      --vendor VENDOR      The account vendor (default = MessageMedia)
      --file FILE          The csv with numbers and accounts to process
      --sleep SLEEP        Wait in milliseconds between batch
      --size SIZE          The amount of analyzed numbers before sleep
      --display DISPLAY    Display metrics every this amount of analyzed numbers

#### CSV file sample:

CSV file should contain column for `number` (exact match) and `account_name`. Do not include headers.

    +111314863,Account_XYZ_0001
    +111314864,Account_XYZ_0002
    +111314865,Account_XYZ_0003

### Example

    $ python assign_number_to_account.py --base-url https://numbers-service-syd.stg.messagemedia.com --vendor MessageMedia --note GATEWAY-2449 --file test.csv
    Fixed Numbers:  3 , Analyzed Numbers:  3 , Total Numbers 3 , Numbers Left:  0 , Error Fixing:  0, Numbers Missing: 0
    Process finished


## Unassign Numbers script

This script unassigns numbers from file.

### Usage

    $ python unassign_number.py -h
    usage: unassign_number.py [-h] --base-url BASE_URL --file FILE
                                 [--sleep SLEEP] [--size SIZE] [--display DISPLAY]
    
    A python script to unassign numbers
    
    optional arguments:
      -h, --help           show this help message and exit
      --base-url BASE_URL  The Base URL of Numbers Service
      --file FILE          The csv with numbers and accounts to process
      --sleep SLEEP        Wait in milliseconds between batch
      --size SIZE          The amount of analyzed numbers before sleep
      --display DISPLAY    Display metrics every this amount of analyzed numbers

#### CSV file sample:

CSV file should contain column for `number` (exact match). Do not include headers.

    +111314863
    +111314864
    +111314865
    +111314866

### Example

    $ python unassign_number.py --base-url https://numbers-service-syd.stg.messagemedia.com --file test.csv
    Fixed Numbers:  4 , Analyzed Numbers:  4 , Total Numbers 4 , Numbers Left:  0 , Error Fixing:  0, Numbers Missing: 0
    Process finished
