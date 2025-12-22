#!/usr/bin/env python3
"""
Apply Realtime Database rules using service account credentials.
Usage:
  python apply_rules.py --sa draw-master-d1ba0-firebase-adminsdk-fbsvc-7aa7eac6f3.json --db-host draw-master-d1ba0-default-rtdb.europe-west1.firebasedatabase.app

Requires: pip install google-auth requests
"""
import argparse
import json
import sys
from google.oauth2 import service_account
from google.auth.transport.requests import Request
import requests

def load_rules(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def get_access_token(sa_path, scopes=None):
    scopes = scopes or [
        "https://www.googleapis.com/auth/firebase.database",
        "https://www.googleapis.com/auth/userinfo.email",
    ]
    creds = service_account.Credentials.from_service_account_file(sa_path, scopes=scopes)
    creds.refresh(Request())
    return creds.token

def apply_rules(sa_path, db_host, rules_path):
    token = get_access_token(sa_path)
    rules = load_rules(rules_path)
    url = f"https://{db_host}/.settings/rules.json?access_token={token}"
    resp = requests.put(url, json=rules)
    print(resp.status_code)
    if resp.status_code not in (200, 204):
        print(resp.text)
        sys.exit(1)
    print("Rules deployed successfully.")

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--sa", required=True, help="Path to service account JSON")
    p.add_argument("--db-host", required=True, help="Realtime DB host (e.g. myproj-default-rtdb.europe-west1.firebasedatabase.app)")
    p.add_argument("--rules", default="firebase/database.rules.json", help="Path to rules file")
    args = p.parse_args()
    apply_rules(args.sa, args.db_host, args.rules)
