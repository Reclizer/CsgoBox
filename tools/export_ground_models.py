from pathlib import Path
import json
repo=Path(__file__).resolve().parents[1]
assets=repo/'src/main/resources/assets/csgobox'
target=assets/'models/block/animated'; target.mkdir(exist_ok=True)
manifest=[]
for folder in sorted((repo/'blockbench/appearances').iterdir()):
    if not folder.is_dir(): continue
    meta=json.loads((folder/'model_manifest.json').read_text(encoding='utf-8'))
    texture=json.loads((assets/f'models/block/boxes/{folder.name}.json').read_text(encoding='utf-8'))['textures']
    parts=folder/'java_models/parts'
    body=json.loads(next(parts.glob('*_body.json')).read_text(encoding='utf-8'))
    # Keep the supplied body, floor and decorative faces exactly as authored.
    lid=json.loads(next(parts.glob('*_lid.json')).read_text(encoding='utf-8'))
    lid['elements']+=json.loads(next(parts.glob('*_lock.json')).read_text(encoding='utf-8'))['elements']
    for name,data in [('body',body),('lid',lid)]:
        data['textures']=texture
        (target/f'{folder.name}_{name}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manifest.append(dict(appearance=folder.name,pivot=meta['pivot_java'],source=parts.relative_to(repo).as_posix(),body=len(body['elements']),lid=len(lid['elements'])))
(repo/'blockbench/ground-animation-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(assets/'models/item/empty_box.json').write_bytes((assets/'models/item/csgo_box.json').read_bytes())
print('Exported five current appearances, preserving original geometry and textures.')
