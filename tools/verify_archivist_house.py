#!/usr/bin/env python3
import gzip, struct, sys
from pathlib import Path
from collections import Counter
P = Path(__file__).resolve().parent.parent / 'src/main/resources/data/mystcraft/structure/village/archivist_house.nbt'
TAG_END=0; TAG_BYTE=1; TAG_SHORT=2; TAG_INT=3; TAG_LONG=4; TAG_FLOAT=5; TAG_DOUBLE=6; TAG_BYTE_ARRAY=7; TAG_STRING=8; TAG_LIST=9; TAG_COMPOUND=10; TAG_INT_ARRAY=11; TAG_LONG_ARRAY=12
class R:
 def __init__(self,b): self.b=b; self.i=0
 def take(self,n): x=self.b[self.i:self.i+n]; self.i+=n; return x
 def u8(self): return self.take(1)[0]
 def i8(self): return struct.unpack('>b',self.take(1))[0]
 def i16(self): return struct.unpack('>h',self.take(2))[0]
 def u16(self): return struct.unpack('>H',self.take(2))[0]
 def i32(self): return struct.unpack('>i',self.take(4))[0]
 def i64(self): return struct.unpack('>q',self.take(8))[0]
 def f32(self): return struct.unpack('>f',self.take(4))[0]
 def f64(self): return struct.unpack('>d',self.take(8))[0]
 def s(self): return self.take(self.u16()).decode()
 def p(self,t):
  if t==TAG_BYTE:return self.i8()
  if t==TAG_SHORT:return self.i16()
  if t==TAG_INT:return self.i32()
  if t==TAG_LONG:return self.i64()
  if t==TAG_FLOAT:return self.f32()
  if t==TAG_DOUBLE:return self.f64()
  if t==TAG_STRING:return self.s()
  if t==TAG_BYTE_ARRAY:
   n=self.i32(); return list(self.take(n))
  if t==TAG_INT_ARRAY:
   n=self.i32(); return [self.i32() for _ in range(n)]
  if t==TAG_LONG_ARRAY:
   n=self.i32(); return [self.i64() for _ in range(n)]
  if t==TAG_LIST:
   et=self.u8(); n=self.i32(); return [self.p(et) for _ in range(n)]
  if t==TAG_COMPOUND:
   d={}
   while True:
    et=self.u8()
    if et==TAG_END:return d
    name=self.s(); d[name]=self.p(et)
  raise ValueError(t)
raw=gzip.open(P,'rb').read(); r=R(raw); assert r.u8()==TAG_COMPOUND; assert r.s()==''; root=r.p(TAG_COMPOUND)
assert root['DataVersion']==3955, root['DataVersion']
assert root['size']==[9,12,7], root['size']
pal=root['palette']; names=[x['Name'] for x in pal]; blocks=root['blocks']
count=Counter(names[b['state']] for b in blocks)

doors=[b for b in blocks if names[b['state']]=='minecraft:oak_door']
assert len(doors)==2,doors
for b in doors:
    props=pal[b['state']].get('Properties',{})
    assert props.get('facing')=='north',('Archivist door must be NORTH in the unrotated modern template', b, props)
assert count['mystcraft:blocklectern']==2,count
assert count['mystcraft:writingdesk']==2,count
assert count['minecraft:jigsaw']==1,count
lect=[b for b in blocks if names[b['state']]=='mystcraft:blocklectern']
assert all(b.get('nbt',{}).get('TreasurePending')==1 for b in lect),lect
j=[b for b in blocks if names[b['state']]=='minecraft:jigsaw'][0]
assert j['nbt']['name']=='minecraft:building_entrance' and j['nbt']['target']=='minecraft:building_entrance',j
# Modern jigsaw entity placement is deliberately not trusted for the Archivist.
# The owning lower-main desk carries a one-shot marker that reconstructs the legacy spawn.
ents=root['entities']; assert len(ents)==0,ents
desks=[b for b in blocks if names[b['state']]=='mystcraft:writingdesk']
main=[]
for b in desks:
    props=pal[b['state']].get('Properties',{})
    if props.get('isfoot')=='false' and props.get('istop')=='false':
        main.append(b)
assert len(main)==1,main
assert main[0].get('nbt',{}).get('id')=='mystcraft:writingdesk',main
assert main[0].get('nbt',{}).get('ArchivistSpawnPending')==1,main
print('ArchivistHouseNBT: PASS')
print('blocks',len(blocks),'palette',len(pal),'counts',dict(count))
