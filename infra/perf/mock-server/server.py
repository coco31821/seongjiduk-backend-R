"""No-key performance stub: fixed latency, AI capacity and request counters."""
import json, os, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock
AI_DELAY_MS=int(os.getenv("AI_DELAY_MS","1000")); EXTERNAL_DELAY_MS=int(os.getenv("EXTERNAL_DELAY_MS","200")); AI_MAX_INFLIGHT=int(os.getenv("AI_MAX_INFLIGHT","4"))
lock=Lock(); inflight=0; max_inflight=0; calls={}
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*_): pass
 def respond(self,status,payload):
  raw=json.dumps(payload).encode(); self.send_response(status); self.send_header("Content-Type","application/json"); self.send_header("Content-Length",str(len(raw))); self.end_headers(); self.wfile.write(raw)
 def do_GET(self):
  global calls
  if self.path=="/debug/stats":
   with lock: return self.respond(200,{"calls":calls,"inflight":inflight,"max_inflight":max_inflight})
  time.sleep(EXTERNAL_DELAY_MS/1000)
  with lock: calls[self.path.split("?")[0]]=calls.get(self.path.split("?")[0],0)+1
  if "/geocode/" in self.path: return self.respond(200,{"status":"OK","results":[{"formatted_address":"Mock Tokyo","address_components":[{"long_name":"Tokyo","types":["locality"]}],"geometry":{"location":{"lat":35.681,"lng":139.767}}}]})
  if "/v1/search/blog.json" in self.path: return self.respond(200,{"items":[{"title":"Mock route","link":"http://mock-api:8081/posts/1","postdate":"20260101"}]})
  if self.path.startswith("/posts/"):
   text = "Mock Shrine A에서 Mock Shrine B까지 이동한 성지순례 동선과 방문 순서를 기록한 후기입니다. " * 12
   raw=("<html><body>"+text+"</body></html>").encode(); self.send_response(200); self.send_header("Content-Type","text/html"); self.send_header("Content-Length",str(len(raw))); self.end_headers(); return self.wfile.write(raw)
  return self.respond(200,{"status":"OK"})
 def do_POST(self):
  global inflight,max_inflight,calls
  raw_body=self.rfile.read(int(self.headers.get("Content-Length","0"))); path=self.path
  if path == "/debug/reset":
   with lock:
    calls = {}
    max_inflight = inflight
   return self.respond(200,{"calls":calls,"inflight":inflight,"max_inflight":max_inflight})
  with lock: calls[path]=calls.get(path,0)+1
  if path.startswith("/ai/"):
   with lock:
    if inflight>=AI_MAX_INFLIGHT: return self.respond(503,{"error":"AI capacity exceeded"})
    inflight+=1; max_inflight=max(max_inflight,inflight)
   try:
    time.sleep(AI_DELAY_MS/1000)
    if path=="/ai/trips/generate": return self.respond(200,{"title":"Mock trip","days":[],"shareText":"mock","provider":"perf-stub"})
    if path=="/ai/spots/describe":
     request=json.loads(raw_body or b"{}")
     descriptions=[{"spotId":spot["id"],"sceneDescription":"Mock scene","specialPoint":"Mock point","koreanName":spot["name"],"recommendedMinutes":45,"missions":[]} for spot in request.get("spots",[])]
     return self.respond(200,{"contentId":1,"provider":"perf-stub","descriptions":descriptions})
    return self.respond(200,{"provider":"perf-stub","postCount":1,"usedPostCount":1,"spotMentions":[{"spotId":1,"count":1,"postIndexes":[0]},{"spotId":2,"count":1,"postIndexes":[0]}],"verifiedPairs":[{"fromSpotId":1,"toSpotId":2,"count":1}],"courses":[{"rank":1,"spotIds":[1,2],"supportCount":1,"postIndexes":[0]}],"spotTips":[]})
   finally:
    with lock: inflight-=1
  time.sleep(EXTERNAL_DELAY_MS/1000)
  return self.respond(200,{"places":[{"displayName":{"text":"Mock Place"},"primaryTypeDisplayName":{"text":"Mock"},"rating":4.5,"userRatingCount":100,"location":{"latitude":35.681,"longitude":139.767},"googleMapsUri":"http://mock/maps"}]})
ThreadingHTTPServer(("0.0.0.0",8081),Handler).serve_forever()
