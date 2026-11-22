#!/usr/bin/env python3
"""Discover question-bearing HTML pages on configured official portals.
Checks robots.txt, stays on configured domains, limits requests, and never bypasses access controls.
Copies page text into a draft only when source config explicitly says reusePermission='approved'.
"""
import json,re,time
from collections import deque
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse,urldefrag
from urllib.robotparser import RobotFileParser
import requests
from bs4 import BeautifulSoup
ROOT=Path(__file__).resolve().parents[1]
CONFIG=ROOT/'sources.json'
OUT=ROOT/'data'/'html-question-discoveries.json'
UA='ALL-EXAM-INDIA-PYQ-Monitor/1.2 (official-page discovery)'
Q=re.compile(r'(?:^|\s)(?:Q(?:uestion)?\s*)?\d{1,3}[.)、:]\s+|which of the following|who among the following|खालीलपैकी|भारत के संविधान',re.I)
LINK=re.compile(r'question|paper|answer|previous|archive|exam|notice|recruitment|प्रश्न|उत्तर',re.I)
def clean(u): return urldefrag(u)[0]
def allowed(u,ds):
 p=urlparse(u); h=(p.hostname or '').lower()
 return p.scheme in ('http','https') and any(h==d or h.endswith('.'+d) for d in ds)
def main():
 cfg=json.loads(CONFIG.read_text(encoding='utf-8')); s=requests.Session(); s.headers['User-Agent']=UA
 robots={}; seen=set(); found=[]; errors=[]; count=0; now=datetime.now(timezone.utc).isoformat()
 def can_fetch(u):
  p=urlparse(u); root=f'{p.scheme}://{p.netloc}'
  if root not in robots:
   rp=RobotFileParser(); rp.set_url(root+'/robots.txt')
   try: rp.read(); robots[root]=rp
   except Exception: robots[root]=None
  return bool(robots[root] and robots[root].can_fetch(UA,u))
 for src in cfg.get('sources',[]):
  if not src.get('enabled'): continue
  domains=src.get('allowedDomains',[]); q=deque([(src['url'],0)]); n=0
  while q and n<30:
   u,d=q.popleft(); u=clean(u)
   if u in seen or not allowed(u,domains): continue
   seen.add(u)
   if not can_fetch(u): continue
   try:
    r=s.get(u,timeout=18)
    if r.status_code in (401,403,429):
     errors.append({'sourceId':src['id'],'url':u,'status':r.status_code,'action':'stopped'}); continue
    r.raise_for_status()
    if 'text/html' not in r.headers.get('Content-Type','').lower(): continue
    n+=1; count+=1; soup=BeautifulSoup(r.text,'html.parser')
    for x in soup(['script','style','nav','footer','header','noscript']): x.decompose()
    txt=' '.join(soup.get_text(' ',strip=True).split()); matches=len(Q.findall(txt))
    title=soup.title.get_text(' ',strip=True)[:200] if soup.title else ''
    if matches>=2:
     row={'sourceId':src['id'],'sourceName':src['name'],'examFamilies':src.get('examFamilies',[]),'url':r.url,'title':title,'questionPatternMatches':matches,'checkedAt':now,'reusePermission':src.get('reusePermission','not_reviewed'),'status':'discovered_needs_exam_year_answer_and_rights_review'}
     if (src.get('reusePermission')=='approved' and src.get('permissionEvidenceUrl')
                        and src.get('permissionBasis') and src.get('permissionReviewedAt')):
      row['textDraft']=txt[:100000]; row['status']='text_draft_needs_exam_year_and_answer_review'
     found.append(row)
    if d<2:
     for a in soup.find_all('a',href=True):
      v=clean(urljoin(r.url,a['href'].strip()))
      if allowed(v,domains) and LINK.search(a.get_text(' ',strip=True)+' '+v): q.append((v,d+1))
    time.sleep(1)
   except Exception as e: errors.append({'sourceId':src['id'],'url':u,'error':str(e)[:200]})
 OUT.parent.mkdir(parents=True,exist_ok=True)
 OUT.write_text(json.dumps({'checkedAt':now,'pagesChecked':count,'questionPagesFound':len(found),'pages':found,'errors':errors,'note':'Discovery is not proof of a real PYQ or permission to republish.'},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps({'pagesChecked':count,'questionPagesFound':len(found),'errors':len(errors)}))
if __name__=='__main__': main()
