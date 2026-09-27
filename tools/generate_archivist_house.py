#!/usr/bin/env python3
"""Generate the Mystcraft 0.13.7.06 Archivist House as a 1.21.1 structure template.

The geometry follows ComponentVillageArchivistHouse exactly where the old
StructureBoundingBox allowed placement. Runtime-only behavior is carried by
block/entity NBT:
- two Mystcraft lecterns start with TreasurePending=1 and roll Rank >= 3 pages
  from the live symbol registry when their block entities first load;
- the lower-main Writing Desk carries ArchivistSpawnPending=1 and spawns the legacy
  Archivist once its block entity becomes live;
- one building_entrance jigsaw connects the house to vanilla village roads.
"""
from __future__ import annotations
import gzip, io, os, struct
from collections import OrderedDict

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "mystcraft", "structure", "village", "archivist_house.nbt")

# Minimal big-endian Java NBT writer -------------------------------------------------
TAG_END=0; TAG_BYTE=1; TAG_SHORT=2; TAG_INT=3; TAG_LONG=4; TAG_FLOAT=5; TAG_DOUBLE=6
TAG_BYTE_ARRAY=7; TAG_STRING=8; TAG_LIST=9; TAG_COMPOUND=10; TAG_INT_ARRAY=11; TAG_LONG_ARRAY=12

def _s(v:str)->bytes:
    b=v.encode('utf-8'); return struct.pack('>H', len(b))+b

def payload(tag_type:int, v):
    if tag_type==TAG_BYTE: return struct.pack('>b', int(v))
    if tag_type==TAG_INT: return struct.pack('>i', int(v))
    if tag_type==TAG_LONG: return struct.pack('>q', int(v))
    if tag_type==TAG_DOUBLE: return struct.pack('>d', float(v))
    if tag_type==TAG_STRING: return _s(v)
    if tag_type==TAG_INT_ARRAY:
        return struct.pack('>i',len(v))+b''.join(struct.pack('>i',int(x)) for x in v)
    if tag_type==TAG_LIST:
        elem_type, vals = v
        return bytes([elem_type])+struct.pack('>i',len(vals))+b''.join(payload(elem_type,x) for x in vals)
    if tag_type==TAG_COMPOUND:
        out=bytearray()
        for name,(t,val) in v.items():
            out.append(t); out.extend(_s(name)); out.extend(payload(t,val))
        out.append(TAG_END); return bytes(out)
    raise ValueError(tag_type)

def named_root(compound):
    return bytes([TAG_COMPOUND])+_s('')+payload(TAG_COMPOUND, compound)

def C(**kwargs):
    """Compound from name=(tagtype,value). Keeps insertion order for readable dumps."""
    return OrderedDict(kwargs)

def props(**kwargs):
    return C(**{k:(TAG_STRING,str(v)) for k,v in kwargs.items()})

# Structure authoring ----------------------------------------------------------------
blocks = {}  # (x,y,z) -> (name, props, optional BE nbt)

def setb(x,y,z,name,p=None,nbt=None):
    if 0 <= x < 9 and 0 <= y < 12 and 0 <= z < 7:
        blocks[(x,y,z)] = (name, p or {}, nbt)

def fill(x1,y1,z1,x2,y2,z2,name,p=None):
    for x in range(x1,x2+1):
        for y in range(y1,y2+1):
            for z in range(z1,z2+1):
                setb(x,y,z,name,p)

# Old generator first clears the internal volume. Structure templates do not need
# explicit air everywhere, but we include the deliberate interior air so village
# terrain/vegetation cannot remain inside the house.
fill(1,1,1,8,6,6,'minecraft:air')
# Floor/walls.
fill(1,0,1,7,0,5,'minecraft:oak_planks')
fill(0,0,0,0,3,5,'minecraft:cobblestone')
fill(0,0,0,7,3,0,'minecraft:cobblestone')
fill(1,2,0,7,2,0,'minecraft:oak_planks')
fill(8,0,0,8,3,5,'minecraft:cobblestone')
fill(8,2,1,8,2,5,'minecraft:oak_planks')
fill(8,3,2,8,3,4,'minecraft:oak_planks')
fill(0,0,6,8,3,6,'minecraft:cobblestone')
# Roof body.
fill(0,6,3,8,6,3,'minecraft:oak_planks')
fill(0,5,2,8,5,4,'minecraft:oak_planks')
fill(0,4,1,8,4,5,'minecraft:oak_planks')
fill(2,4,2,6,4,4,'minecraft:air')
# Old stair loops with out-of-bounds i=-1 placements naturally clipped.
for i in range(-1,3):
    for x in range(9):
        setb(x,4+i,i,'minecraft:oak_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
        setb(x,4+i,6-i,'minecraft:oak_stairs',{'facing':'south','half':'bottom','shape':'straight','waterlogged':'false'})
# Windows.
setb(2,2,0,'minecraft:glass_pane',{'east':'true','north':'false','south':'false','waterlogged':'false','west':'true'})
setb(3,2,0,'minecraft:glass_pane',{'east':'true','north':'false','south':'false','waterlogged':'false','west':'true'})
for z in (2,3,4):
    setb(8,2,z,'minecraft:glass_pane',{'east':'false','north':'true','south':'true','waterlogged':'false','west':'false'})
# Door. The 1.12 helper's WEST argument is not the modern DoorBlock FACING contract.
# In this unrotated template the entrance is in the north (z=0) wall, so a closed
# modern door must face NORTH/SOUTH; NORTH points outward and lets StructureTemplate
# rotate the state naturally with the piece. Keeping WEST made open=false look open
# and caused villagers to repeatedly toggle the doorway into the blocking position.
setb(6,1,0,'minecraft:oak_door',{'facing':'north','half':'lower','hinge':'left','open':'false','powered':'false'})
setb(6,2,0,'minecraft:oak_door',{'facing':'north','half':'upper','hinge':'left','open':'false','powered':'false'})
# Internal lights/furniture.
setb(1,3,1,'minecraft:wall_torch',{'facing':'north'})
setb(6,4,2,'minecraft:wall_torch',{'facing':'north'})
setb(5,3,5,'minecraft:wall_torch',{'facing':'south'})
setb(6,1,4,'minecraft:oak_fence',{'east':'false','north':'false','south':'false','waterlogged':'false','west':'false'})
setb(6,2,4,'minecraft:oak_pressure_plate',{'powered':'false'})
setb(6,1,5,'minecraft:oak_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
setb(7,1,5,'minecraft:oak_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
setb(7,1,4,'minecraft:oak_stairs',{'facing':'east','half':'bottom','shape':'straight','waterlogged':'false'})
setb(1,1,1,'minecraft:oak_planks'); setb(1,1,2,'minecraft:oak_planks')
for p in ((1,1,3),(1,2,3),(1,1,4),(1,2,4),(2,1,5),(2,2,5),(3,1,5),(3,2,5)):
    setb(*p,'minecraft:bookshelf')
setb(1,1,5,'minecraft:oak_planks'); setb(1,2,5,'minecraft:oak_planks')

# Writing Desk: old placeDeskAt(4,1,1 -> 4,1,2). Current desk's FACING is the
# direction from main to foot, hence SOUTH for the unrotated template.
desk_common={'facing':'south','istop':'false'}
# Template entity placement proved unreliable inside modern village jigsaws.  Put a one-shot
# marker on the real owning block entity instead; WritingDeskBlockEntity reconstructs the old
# local spawn (4,2,4) after StructureTemplate has transformed the desk's FACING.
be_desk=C(id=(TAG_STRING,'mystcraft:writingdesk'), ArchivistSpawnPending=(TAG_BYTE,1))
setb(4,1,1,'mystcraft:writingdesk',{**desk_common,'isfoot':'false'},be_desk)
setb(4,1,2,'mystcraft:writingdesk',{**desk_common,'isfoot':'true'})

# Two old Archivist lecterns. TreasurePending is a modern one-shot bridge that rolls
# legacy Rank>=3 treasure from the *runtime* symbol pool (including mod biomes/fluids).
be_lectern=C(id=(TAG_STRING,'mystcraft:book_display'), TreasurePending=(TAG_BYTE,1))
setb(1,2,1,'mystcraft:blocklectern',{'facing':'east'},be_lectern)
setb(1,2,2,'mystcraft:blocklectern',{'facing':'east'},be_lectern)

# Jigsaw connector at the foundation under the doorway. Once consumed it becomes the
# cobblestone block that occupied this exact coordinate in the old component.
jigsaw_nbt=C(
    id=(TAG_STRING,'minecraft:jigsaw'),
    name=(TAG_STRING,'minecraft:building_entrance'),
    target=(TAG_STRING,'minecraft:building_entrance'),
    pool=(TAG_STRING,'minecraft:empty'),
    final_state=(TAG_STRING,'minecraft:cobblestone'),
    joint=(TAG_STRING,'rollable'),
)
setb(6,0,0,'minecraft:jigsaw',{'orientation':'north_up'},jigsaw_nbt)

# Palette de-duplication.
palette=[]; pindex={}; out_blocks=[]
for (x,y,z),(name,pr,nbt) in sorted(blocks.items(), key=lambda kv:(kv[0][1],kv[0][2],kv[0][0])):
    key=(name,tuple(sorted(pr.items())))
    if key not in pindex:
        pindex[key]=len(palette)
        pc=C(Name=(TAG_STRING,name))
        if pr:
            pc['Properties']=(TAG_COMPOUND, props(**pr))
        palette.append(pc)
    bc=C(pos=(TAG_LIST,(TAG_INT,[x,y,z])), state=(TAG_INT,pindex[key]))
    if nbt is not None:
        # Clone so repeated lecterns don't share mutable OrderedDicts in future edits.
        bc['nbt']=(TAG_COMPOUND, OrderedDict(nbt))
    out_blocks.append(bc)

# Do not embed a template entity here.  The main Writing Desk's one-shot marker is the
# authoritative spawn path; keeping an NBT entity as well could duplicate the shopkeeper.

root=C(
    DataVersion=(TAG_INT,3955),
    size=(TAG_LIST,(TAG_INT,[9,12,7])),
    palette=(TAG_LIST,(TAG_COMPOUND,palette)),
    blocks=(TAG_LIST,(TAG_COMPOUND,out_blocks)),
    entities=(TAG_LIST,(TAG_COMPOUND,[])),
)
raw=named_root(root)
os.makedirs(os.path.dirname(OUT),exist_ok=True)
with gzip.open(OUT,'wb',compresslevel=9,mtime=0) if False else open(os.devnull,'wb') as _:
    pass
# gzip.GzipFile supports deterministic mtime across Python versions.
with open(OUT,'wb') as f:
    with gzip.GzipFile(filename='',mode='wb',fileobj=f,compresslevel=9,mtime=0) as gz:
        gz.write(raw)
print(f"wrote {OUT}")
print(f"blocks={len(out_blocks)} palette={len(palette)} entities=0 bytes={os.path.getsize(OUT)}")
