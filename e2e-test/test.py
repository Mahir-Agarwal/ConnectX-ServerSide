import requests
import asyncio
import websockets
import json
import sys

# CONFIGURATION
BASE_URL = "https://committable-chong-desperately.ngrok-free.dev"
EUREKA_URL = "https://committable-chong-desperately.ngrok-free.dev/eureka" # Usually not exposed, but for reference
WS_URL = "wss://committable-chong-desperately.ngrok-free.dev/ws"  # Ngrok uses https/wss

def print_result(name, success, message=""):
    if success:
        print(f"✅ [PASS] {name} {message}")
    else:
        print(f"❌ [FAIL] {name} - {message}")
        sys.exit(1)

def test_http_endpoints():
    print(f"\n--- Testing HTTP Endpoints ({BASE_URL}) ---\n")
    
    # 1. Create Session
    session_data = {}
    try:
        url = f"{BASE_URL}/api/sessions"
        print(f"Sending POST to {url}...")
        resp = requests.post(url)
        
        if resp.status_code == 200:
            data = resp.json()
            if "sessionId" in data and "senderToken" in data and "receiverToken" in data:
                session_data = data
                print_result("Create Session", True, f"ID: {data['sessionId']}")
            else:
                print_result("Create Session", False, "Missing keys in response")
        else:
            print_result("Create Session", False, f"Status: {resp.status_code}")
            
    except Exception as e:
        print_result("Create Session", False, f"Exception: {str(e)}")
        return None

    # 2. Get Session
    try:
        sid = session_data['sessionId']
        url = f"{BASE_URL}/api/sessions/{sid}"
        resp = requests.get(url)
        
        if resp.status_code == 200:
            print_result("Get Session", True)
        else:
            print_result("Get Session", False, f"Status: {resp.status_code}")
            
    except Exception as e:
        print_result("Get Session", False, str(e))

    # 3. Validate Session
    try:
        sid = session_data['sessionId']
        url = f"{BASE_URL}/api/sessions/{sid}/validate"
        resp = requests.get(url)
        
        if resp.status_code == 200:
            print_result("Validate Session", True, f"Response: {resp.text}")
        else:
            print_result("Validate Session", False, f"Status: {resp.status_code}")

    except Exception as e:
        print_result("Validate Session", False, str(e))
        
    except Exception as e:
        print_result("Validate Session", False, str(e))

    # 4. Get TURN Credentials
    try:
        url = f"{BASE_URL}/api/turn-credentials" # Note: This might need port 8082 if SignalingService is separate, but Gateway maps it.
        # Assuming Gateway maps /api/turn-credentials via SignalingService or it's accessible.
        # Wait, usually SignalingService is on a different port or mapped via Gateway. 
        # Checking test.py CONFIGURATION: BASE_URL = "http://10.143.245.98:8080" (Gateway?)
        
        # If /api/turn-credentials is in SignalingService, we need to ensure Gateway routes it.
        # The prompt implies we put it in SignalingService.
        # Let's assume the Gateway routes /api/* to the appropriate services or we might need to hit Signaling directly if Gateway isn't configured for this new route yet.
        # However, for this test, we'll try the BASE_URL.
        
        print(f"Sending GET to {url}...")
        resp = requests.get(url)
        
        if resp.status_code == 200:
            data = resp.json()
            if "iceServers" in data and len(data["iceServers"]) > 0:
                policy = data.get("iceTransportPolicy", "missing")
                print_result("Get TURN Crede", True, f"Servers: {len(data['iceServers'])}, Policy: {policy}")
            else:
                print_result("Get TURN Crede", False, f"Invalid format: {data}")
        else:
            print_result("Get TURN Crede", False, f"Status: {resp.status_code} (Ensure Gateway routes this or use Service Port)")

    except Exception as e:
        print_result("Get TURN Crede", False, str(e))
        
    return session_data

async def test_websockets(session_data):
    print(f"\n--- Testing WebSockets ({WS_URL}) ---\n")
    
    sender_token = session_data['senderToken']
    receiver_token = session_data['receiverToken']
    session_id = session_data['sessionId']

    sender_uri = f"{WS_URL}?token={sender_token}"
    receiver_uri = f"{WS_URL}?token={receiver_token}"

    try:
        # 1. Connect Sender
        async with websockets.connect(sender_uri) as sender_ws:
            print_result("Sender Connect", True)
            
            # Send Join as Sender
            join_msg = {
                "type": "JOIN_AS_SENDER",
                "sessionId": session_id
            }
            await sender_ws.send(json.dumps(join_msg))
            print("   -> Sent JOIN_AS_SENDER")
            
            # 2. Connect Receiver
            async with websockets.connect(receiver_uri) as receiver_ws:
                print_result("Receiver Connect", True)
                
                # Send Join as Receiver
                join_msg_rx = {
                    "type": "JOIN_AS_RECEIVER",
                    "sessionId": session_id
                }
                await receiver_ws.send(json.dumps(join_msg_rx))
                print("   -> Sent JOIN_AS_RECEIVER")
                
                # 3. Verify Receiver gets ACK
                rx_response = await asyncio.wait_for(receiver_ws.recv(), timeout=5)
                rx_json = json.loads(rx_response)
                
                if rx_json.get("type") == "JOIN_ACK":
                    print_result("Receiver JOIN_ACK", True)
                else:
                    print_result("Receiver JOIN_ACK", False, f"Got: {rx_json}")

                # 4. Verify Sender gets RECEIVER_JOINED
                tx_response = await asyncio.wait_for(sender_ws.recv(), timeout=5)
                tx_json = json.loads(tx_response)
                
                if tx_json.get("type") == "RECEIVER_JOINED":
                    print_result("Sender RECEIVER_JOINED", True)
                else:
                    print_result("Sender RECEIVER_JOINED", False, f"Got: {tx_json}")

    except Exception as e:
        print_result("WebSocket Test", False, str(e))

async def main():
    print("🚀 Starting Backend E2E Test...")
    session_data = test_http_endpoints()
    
    if session_data:
        await test_websockets(session_data)
        print("\n✅ ALL TESTS PASSED SUCCESSFULLY!")
    else:
        print("\n❌ SKIPPING WEBSOCKET TESTS DUE TO HTTP FAILURE")

if __name__ == "__main__":
    asyncio.run(main())
