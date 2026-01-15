import subprocess
import time
import os

def run_and_capture(cmd, cwd, duration=20):
    print(f"Running {cmd} in {cwd}...")
    with open("debug_log.txt", "w") as f:
        process = subprocess.Popen(
            cmd,
            cwd=cwd,
            shell=True,
            stdout=f,
            stderr=f
        )
        time.sleep(duration)
        process.kill()
        print("Done.")

if __name__ == "__main__":
    run_and_capture(".\\mvnw.cmd spring-boot:run", "m:/ConnectX/SessionService/SessionService")
