from pathlib import Path
import json,copy
repo=Path(__file__).resolve().parents[1]
assets=repo/'src/main/resources/assets/csgobox'
target=assets/'models/block/animated'; target.mkdir(exist_ok=True)
def crop(element,lower,upper):
    e=copy.deepcopy(element); e['from']=lower; e['to']=upper
    axes={'up':[(0,False),(2,False)],'down':[(0,False),(2,True)],'north':[(0,True),(1,True)],
          'south':[(0,False),(1,True)],'west':[(2,False),(1,True)],'east':[(2,True),(1,True)]}
    for face,data in e['faces'].items():
        if 'uv' not in data: continue
        uv=data['uv']; ranges=[]
        for axis,reverse in axes[face]:
            lo,hi=element['from'][axis],element['to'][axis]; size=hi-lo
            if not size: ranges.append((0,1)); continue
            ranges.append(((hi-upper[axis])/size,(hi-lower[axis])/size) if reverse else ((lower[axis]-lo)/size,(upper[axis]-lo)/size))
        data['uv']=[uv[0]+(uv[2]-uv[0])*ranges[0][0],uv[1]+(uv[3]-uv[1])*ranges[1][0],
                    uv[0]+(uv[2]-uv[0])*ranges[0][1],uv[1]+(uv[3]-uv[1])*ranges[1][1]]
    return e

def hollow(elements,lower,upper):
    """Subtract an axis-aligned cavity, including decorative plates across its mouth."""
    result=[]
    for element in elements:
        lo,hi=element['from'],element['to']
        cut_lo=[max(lo[i],lower[i]) for i in range(3)]
        cut_hi=[min(hi[i],upper[i]) for i in range(3)]
        if any(cut_lo[i]>cut_hi[i] or (cut_lo[i]==cut_hi[i] and hi[i]>lo[i]) for i in range(3)):
            result.append(element)
            continue
        if element.get('rotation',{}).get('angle',0):
            raise ValueError('Cannot carve a rotated element: '+element.get('name','unnamed'))
        remaining_lo=list(lo); remaining_hi=list(hi)
        for axis in range(3):
            if remaining_lo[axis]<cut_lo[axis]:
                slab_hi=list(remaining_hi); slab_hi[axis]=cut_lo[axis]
                result.append(crop(element,list(remaining_lo),slab_hi))
                remaining_lo[axis]=cut_lo[axis]
            if remaining_hi[axis]>cut_hi[axis]:
                slab_lo=list(remaining_lo); slab_lo[axis]=cut_hi[axis]
                result.append(crop(element,slab_lo,list(remaining_hi)))
                remaining_hi[axis]=cut_hi[axis]
    return result

def validate_panel(elements,pivot,center_z):
    panel_y=max(.035*16,pivot[1]-.018*16)
    for element in elements:
        lo,hi=element['from'],element['to']
        if (lo[0]<8<hi[0] and lo[2]<center_z<hi[2] and
                hi[1]>=panel_y and lo[1]<=pivot[1] and 'up' in element['faces']):
            raise ValueError('Body occludes the reel: '+element.get('name','unnamed'))

manifest=[]
for folder in sorted((repo/'blockbench/appearances').iterdir()):
    if not folder.is_dir(): continue
    meta=json.loads((folder/'model_manifest.json').read_text(encoding='utf-8'))
    texture=json.loads((assets/f'models/block/boxes/{folder.name}.json').read_text(encoding='utf-8'))['textures']
    parts=folder/'java_models/parts'
    body=json.loads(next(parts.glob('*_body.json')).read_text(encoding='utf-8'))
    # The provided Java snapshots have sealed main cuboids. Preserve their skin while making a real cavity.
    main=body['elements'][0]; lo,hi=main['from'],main['to']
    wall=.55; floor=lo[1]+min(.6,(hi[1]-lo[1])*.3)
    body['elements']=hollow(body['elements'],[lo[0]+wall,floor,lo[2]+wall],
                            [hi[0]-wall,meta['pivot_java'][1],hi[2]-wall])
    validate_panel(body['elements'],meta['pivot_java'],(lo[2]+hi[2])/2)
    lid=json.loads(next(parts.glob('*_lid.json')).read_text(encoding='utf-8'))
    lid['elements']+=json.loads(next(parts.glob('*_lock.json')).read_text(encoding='utf-8'))['elements']
    for name,data in [('body',body),('lid',lid)]:
        data['textures']=texture
        (target/f'{folder.name}_{name}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manifest.append(dict(appearance=folder.name,pivot=meta['pivot_java'],source=str(parts.relative_to(repo)),body=len(body['elements']),lid=len(lid['elements'])))
classic=json.loads((assets/'models/item/boxes/classic.json').read_text(encoding='utf-8'))
body=[]; lid=[]
for e in classic['elements']:
    lo,hi=e['from'],e['to']
    if hi[1]>6.6:
        lower=list(lo); lower[1]=max(lo[1],6.6); lid.append(crop(e,lower,list(hi)))
    if lo[1]>=6.6: continue
    upper=list(hi); upper[1]=min(hi[1],6.6)
    body.append(crop(e,list(lo),upper))
body=hollow(body,[1.6,.7,2.6],[14.4,6.6,12.4])
validate_panel(body,[8,6.6,13.4],7.5)
for name,elements in [('body',body),('lid',lid)]:
    (target/f'classic_{name}.json').write_text(json.dumps(dict(credit='Derived from the existing approved classic model; hollow body and hinged lid',textures=classic['textures'],elements=elements),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest.append(dict(appearance='classic',pivot=[8,6.6,13.4],source='models/item/boxes/classic.json',body=len(body),lid=len(lid)))
(repo/'blockbench/ground-animation-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(assets/'models/item/empty_box.json').write_bytes((assets/'models/item/csgo_box.json').read_bytes())
print('Prepared animated model parts, unchanged textures and empty-case model.')
