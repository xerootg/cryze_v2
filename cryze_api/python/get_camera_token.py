import json
import time
import wyze_sdk
from wyze_sdk.service.base import WpkNetServiceClient
import sys
import os
import wyze_sdk.errors

wyze_sdk.set_file_logger('wyze_sdk', filepath='wyze_out.txt', level=wyze_sdk.logging.DEBUG)

mars_base_url = os.getenv('WYZE_MARS_BASE', 'https://wyze-mars-service.wyzecam.com')

email = os.getenv('WYZE_USERNAME')
if not email:
    raise ValueError("WYZE_USERNAME environment variable is not set")

psswd = os.getenv('WYZE_PASSWORD')
if not psswd:
    raise ValueError("WYZE_PASSWORD environment variable is not set")

key_id = os.getenv('WYZE_KEY_ID')
if not key_id:
    raise ValueError("WYZE_KEY_ID environment variable is not set")

api_key = os.getenv('WYZE_API_KEY')
if not api_key:
    raise ValueError("WYZE_API_KEY environment variable is not set")

def do_login():
    if not os.path.exists("tokens.txt"): # If the tokens file doesn't exist, login and save the tokens to a file
        client = wyze_sdk.Client()
        response = client.login(
            email=email,
            password=psswd,
            key_id=key_id,
            api_key=api_key,
    #       totp_key='734700'
        )

        if(response is None or not response.status_code == 200):
            raise ValueError("Login failed")
        
        # Save the tokens to a file for later use
        if client._token and client._refresh_token:
            with open("tokens.txt", "w") as file:
                file.write(client._token + "\n")
                file.write(client._refresh_token + "\n")

        return client
    else:
        try: # Try to read the tokens from the file, if it fails, remove the file and re-login
            with open("tokens.txt", "r") as file:
                access_token = file.readline().strip()
                refresh_token = file.readline().strip()
            # Use tokens to create a client
            client = wyze_sdk.Client(token=access_token, refresh_token=refresh_token)

            # Refresh the access token if needed
            client.refresh_token()

            return client
        except wyze_sdk.errors.WyzeClientError: # only catch wyze_sdk errors, let other exceptions bubble up
            os.remove("tokens.txt")
            return do_login()

def get_camera_token(deviceId):
    if not deviceId or not isinstance(deviceId, str):
        raise ValueError("deviceId must be a non-null string")

    # cache the tokens
    client = do_login()

    wpk = WpkNetServiceClient(token=client._token, base_url=mars_base_url)
    nonce = wpk.request_verifier.clock.nonce()
    json_dict = {"ttl_minutes" : 10080, 'nonce' : str(nonce), 'unique_id' : wpk.phone_id }
    header_dict = { 'appid' : wpk.app_id}

    mars_path_url = f"/plugin/mars/v2/regist_gw_user/{deviceId}"

    resp = wpk.api_call(api_method=mars_path_url, json=json_dict, headers=header_dict, nonce=str(nonce))

    accessId = resp["data"]["accessId"]
    accessToken = resp["data"]["accessToken"]
    expireTime = resp["data"]["expireTime"]

    chat_message = {
        "accessId": "".join(accessId),
        "accessToken": accessToken,
        "expireTime": expireTime,
        "deviceId": deviceId,
        "timestamp": int(time.time()) # the expiry is int, so theres no reason to have this be a float
    }

    print(json.dumps(chat_message))

if __name__ == "__main__":
    if len(sys.argv) < 1:
        raise ValueError("Usage: python get_camera_token.py <device_id>")

    get_camera_token(sys.argv[1])