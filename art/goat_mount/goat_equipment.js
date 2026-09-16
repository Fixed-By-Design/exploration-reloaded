// Load this local authoring tool in Blockbench. It builds native Modded Entity projects,
// paints their pixel atlases, and exports the editable models and Java geometry.
// The matching localhost helper only accepts this project's named output assets.
(function () {
    const endpoint = 'http://127.0.0.1:8129';
    const parts = {
        body: {origin: [0, 24, 0]},
        head: {origin: [1, 14, 0]},
        left_hind_leg: {origin: [1, 14, 4]},
        right_hind_leg: {origin: [-3, 14, 4]},
        left_front_leg: {origin: [1, 14, -6]},
        right_front_leg: {origin: [-3, 14, -6]}
    };
    const saddle = [
        ['blanket', 'body', [-5, -18, -1], [11, 1, 10], 'cloth'],
        ['seat', 'body', [-3, -19, 0], [7, 1, 7], 'leather'],
        ['pommel', 'body', [-4, -20, -1], [9, 2, 2], 'leather'],
        ['cantle', 'body', [-4, -20, 6], [9, 2, 2], 'leather'],
        ['left_girth', 'body', [5, -17, 0], [1, 11, 2], 'strap'],
        ['right_girth', 'body', [-5, -17, 0], [1, 11, 2], 'strap'],
        ['belly_girth', 'body', [-4, -7, 0], [9, 1, 2], 'strap'],
        ['left_stirrup_front', 'body', [6, -14, 1], [1, 4, 1], 'metal'],
        ['left_stirrup_back', 'body', [6, -14, 4], [1, 4, 1], 'metal'],
        ['left_stirrup_foot', 'body', [6, -10, 1], [1, 1, 4], 'metal'],
        ['right_stirrup_front', 'body', [-6, -14, 1], [1, 4, 1], 'metal'],
        ['right_stirrup_back', 'body', [-6, -14, 4], [1, 4, 1], 'metal'],
        ['right_stirrup_foot', 'body', [-6, -10, 1], [1, 1, 4], 'metal']
    ];
    const armor = [
        ['breastplate', 'body', [-4, -16, -9], [9, 8, 1], 'armor'],
        ['left_shoulder', 'body', [6, -18, -7], [1, 8, 8], 'armor'],
        ['right_shoulder', 'body', [-6, -18, -7], [1, 8, 8], 'armor'],
        ['left_flank', 'body', [5, -16, 3], [1, 7, 6], 'armor'],
        ['right_flank', 'body', [-5, -16, 3], [1, 7, 6], 'armor'],
        ['neck_band', 'body', [-5, -19, -7], [11, 1, 3], 'strap'],
        ['rear_band', 'body', [-4, -18, 8], [9, 1, 1], 'strap'],
        ['left_fore_greave', 'left_front_leg', [0, 6, 0], [3, 3, 3], 'armor', 0.18],
        ['right_fore_greave', 'right_front_leg', [0, 6, 0], [3, 3, 3], 'armor', 0.18],
        ['left_hind_greave', 'left_hind_leg', [0, 6, 0], [3, 3, 3], 'armor', 0.18],
        ['right_hind_greave', 'right_hind_leg', [0, 6, 0], [3, 3, 3], 'armor', 0.18]
    ];
    // Main, highlight, shadow and deepest edge sampled from Minecraft 26.1.2 horse armour.
    const palettes = {
        leather: ['#c7c7c7', '#dedede', '#a4a4a4', '#7b7b7b'],
        copper: ['#e77c56', '#fdd4cb', '#ba5d3d', '#a65237'],
        iron: ['#a3a3a3', '#dddddd', '#7b7b7b', '#626262'],
        gold: ['#ffd83d', '#feffbd', '#f5b81c', '#d39632'],
        diamond: ['#43e4d0', '#9af7e3', '#29d0c0', '#17afa1'],
        netherite: ['#49393f', '#766a76', '#3f303b', '#322727'],
        chainmail: ['#8c8c8c', '#bbbbbb', '#626262', '#333333']
    };
    function layout(cubes) {
        let u = 1, v = 1, row = 0;
        return cubes.map(cube => {
            const [dx, dy, dz] = cube[3], width = 2 * (dx + dz), height = dy + dz;
            if (u + width + 1 > 128) { u = 1; v += row + 2; row = 0; }
            const item = {cube, uv: [u, v], width, height};
            u += width + 2; row = Math.max(row, height);
            if (v + height > 128) throw Error('UV atlas overflow');
            return item;
        });
    }
    const saddleUV = layout(saddle), armorUV = layout(armor);
    function paint(entries, material, overlay = false) {
        const canvas = document.createElement('canvas'); canvas.width = canvas.height = 128;
        const ctx = canvas.getContext('2d'); ctx.imageSmoothingEnabled = false;
        const fill = (color,x,y,w=1,h=1) => {ctx.fillStyle=color;ctx.fillRect(x,y,w,h);};
        for (const {cube, uv:[u,v]} of entries) {
            const [name, bone, local, [dx,dy,dz], kind] = cube;
            const dyeable = entries === armorUV && material === 'leather';
            if (dyeable && (overlay ? kind !== 'strap' : kind === 'strap')) continue;
            const colors = kind === 'cloth' ? ['#39696e','#82a4a0','#284c52','#203c42']
                : kind === 'strap' || kind === 'leather' ? ['#804a15','#95652b','#592100','#401800']
                : kind === 'metal' ? ['#a3a3a3','#dddddd','#7b7b7b','#626262'] : palettes[material];
            const faces = [
                [u+dz,v,dx,dz,'top'], [u+dz+dx,v,dx,dz,'bottom'],
                [u,v+dz,dz,dy,'side'], [u+dz,v+dz,dx,dy,'front'],
                [u+dz+dx,v+dz,dz,dy,'side'], [u+2*dz+dx,v+dz,dx,dy,'back']
            ];
            for (const [x,y,w,h,face] of faces) {
                const metal = kind === 'armor' && material !== 'leather' && material !== 'chainmail';
                if (kind === 'armor' && material === 'chainmail') {
                    // Interlocking rings, with actual holes rather than painted grey speckles.
                    for(let py=0;py<h;py++)for(let px=0;px<w;px++) {
                        const rx=(px+(Math.floor(py/3)%2))%3,ry=py%3;
                        if(rx===1&&ry===1)continue;
                        fill(ry===0 ? colors[1] : ry===2 ? colors[3] : colors[0],x+px,y+py);
                    }
                    continue;
                }
                fill(face==='bottom' ? colors[3] : face==='top' ? colors[1] : colors[0],x,y,w,h);
                if(w>2&&h>2) {
                    // Each cube face gets its own bevel. No random noise across UV seams.
                    fill(colors[1],x,y,w,1);fill(colors[2],x,y+1,1,h-1);
                    fill(colors[3],x+w-1,y+1,1,h-1);fill(colors[3],x,y+h-1,w,1);
                    if(h>5)fill(colors[2],x+1,y+h-3,w-2,2);
                    if(metal&&w>4&&h>4) {
                        fill(colors[1],x+1,y+1,Math.max(1,Math.floor(w/2)-1),1);
                        fill(colors[1],x+1,y+2,1,Math.min(2,h-3));
                    }
                    if(kind==='cloth') {
                        fill('#bf9b68',x+1,y+1,w-2,1);
                        fill('#284c52',x+1,y+h-2,w-2,1);
                    }
                    if(kind==='strap'&&w>=5&&face!=='bottom') {
                        const middle=x+Math.floor(w/2);
                        fill('#626262',middle-1,y+1,3,Math.min(3,h-2));
                        fill('#dddddd',middle,y+1,1,1);
                        if(h>=5)fill('#401800',middle,y+2,1,1);
                    }
                    if(kind==='armor'&&h>=7&&(face==='front'||face==='back')) {
                        // A single lower overlap reads as a forged plate, like horse armour.
                        fill(colors[3],x+1,y+h-3,w-2,1);
                        fill(colors[1],x+1,y+h-2,w-2,1);
                    }
                }
            }
        }
        return canvas;
    }
    async function save(name, content) {
        const response = await fetch(endpoint + '/export', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({name, content})});
        if (!response.ok) throw Error('Could not save ' + name);
    }
    function model(name, entries, texture, width = 128) {
        newProject('modded_entity');
        Project.name = name; Project.geometry_name = name; Project.modded_entity_version = '1.17';
        Project.modded_entity_flip_y = true; Project.texture_width = width; Project.texture_height = 128;
        const tex = new Texture({name: name + '.png'}).fromDataURL(texture.toDataURL()).add(false);
        const groups = {};
        for (const [key, def] of Object.entries(parts)) {
            const o = def.origin;
            groups[key] = new Group({name: key, origin: [-o[0], 24-o[1], o[2]]}).init();
        }
        const add = (entry, offset = 0) => {
            const [name, bone, local, size, kind, inflate = 0] = entry.cube;
            const p = parts[bone].origin;
            const mc = local.map((n,i) => n + p[i]);
            const cube = new Cube({name, from: [-mc[0]-size[0], 24-mc[1]-size[1], mc[2]],
                to: [-mc[0], 24-mc[1], mc[2]+size[2]], box_uv: true, autouv: 0,
                uv_offset: [entry.uv[0] + offset, entry.uv[1]], inflate}).addTo(groups[bone]).init();
            cube.applyTexture(tex, true);
            return cube;
        };
        entries.forEach(e => add(e));
        Canvas.updateAll();
        return {groups, add, tex};
    }
    async function build() {
        try {
            const saddleTexture = paint(saddleUV, 'leather');
            const armorTextures = Object.fromEntries(Object.keys(palettes).map(k => [k, paint(armorUV, k)]));
            model('GoatSaddleExport', saddleUV, saddleTexture);
            await save('goat_saddle.bbmodel', Codecs.project.compile());
            await save('GoatSaddleExport.java', Codecs.modded_entity.compile());
            await save('saddle.png', saddleTexture.toDataURL());
            Project.saved = true;
            model('GoatArmorExport', armorUV, armorTextures.iron);
            await save('goat_armor.bbmodel', Codecs.project.compile());
            await save('GoatArmorExport.java', Codecs.modded_entity.compile());
            for (const [name, tex] of Object.entries(armorTextures)) await save(name + '.png', tex.toDataURL());
            await save('leather_overlay.png', paint(armorUV, 'leather', true).toDataURL());
            Project.saved = true;
            const image = new Image(); image.crossOrigin = 'anonymous';
            await new Promise((resolve, reject) => { image.onload = resolve; image.onerror = reject; image.src = endpoint + '/goat.png'; });
            const preview = document.createElement('canvas'); preview.width = 512; preview.height = 128;
            const ctx = preview.getContext('2d'); ctx.drawImage(saddleTexture, 0, 0); ctx.drawImage(armorTextures.iron, 128, 0); ctx.drawImage(image, 256, 0);
            const editor = model('Vanilla goat with mountain equipment', saddleUV, preview, 512);
            armorUV.forEach(e => editor.add(e, 128));
            const vanilla = [
                ['body_fur','body',[-4,-17,-7],[9,11,16],'vanilla'],
                ['shoulder_fur','body',[-5,-18,-8],[11,14,11],'vanilla'],
                ['left_front_leg','left_front_leg',[0,0,0],[3,10,3],'vanilla'],
                ['right_front_leg','right_front_leg',[0,0,0],[3,10,3],'vanilla'],
                ['left_hind_leg','left_hind_leg',[0,4,0],[3,6,3],'vanilla'],
                ['right_hind_leg','right_hind_leg',[0,4,0],[3,6,3],'vanilla'],
                ['right_ear','head',[-6,-11,-10],[3,2,1],'vanilla'],
                ['left_ear','head',[2,-11,-10],[3,2,1],'vanilla'],
                ['left_horn','head',[-0.01,-16,-10],[2,7,2],'vanilla'],
                ['right_horn','head',[-2.99,-16,-10],[2,7,2],'vanilla'],
                ['goatee','head',[-0.5,-3,-14],[0,7,5],'vanilla']
            ];
            const uv = [[1,1],[0,28],[49,2],[35,2],[36,29],[49,29],[2,61],[2,61],[12,55],[12,55],[23,52]];
            vanilla.forEach((cube,i) => editor.add({cube,uv:uv[i]},256));
            const nose = new Group({name:'nose',origin:[-1,18,-8],rotation:[-55,0,0]}).addTo(editor.groups.head).init();
            new Cube({name:'vanilla_nose',from:[-3,15,-16],to:[2,22,-6],box_uv:true,autouv:0,uv_offset:[290,46]}).addTo(nose).init().applyTexture(editor.tex,true);
            Canvas.updateAll();
            Preview.selected.loadAnglePreset({position:[-38,28,-46],target:[0,12,0],projection:'perspective'});
            await save('goat_mount_preview.bbmodel',Codecs.project.compile());
            await save('export-report.json',JSON.stringify({blockbench:Blockbench.version,saddleCubes:saddle.length,armorCubes:armor.length,source:'minecraft:goat, Minecraft 26.1.2',saddleUV,armorUV},null,2));
            Project.saved = true;
            setTimeout(() => Screencam.screenshotPreview(Preview.selected,{crop:false},url => save('preview.png',url)),500);
            Blockbench.showQuickMessage('Chèvre vanilla équipée : modèles et textures exportés',5000);
        } catch(error) {
            console.error(error);
            Blockbench.showMessageBox({title:'Goat equipment export',message:String(error)});
        }
    }
    let action;
    Plugin.register('goat_equipment', {
        title:'Fixed By Design Goat Equipment', author:'Fixed By Design', icon:'pets',
        description:'Creates saddle and armour models fitted to the vanilla goat.',version:'1.1.0',variant:'web',
        onload() {
            action = new Action('build_goat_equipment',{name:'Build vanilla goat equipment',icon:'pets',click:build});
            MenuBar.addAction(action,'tools');
        },
        onunload(){action?.delete();}
    });
})();
