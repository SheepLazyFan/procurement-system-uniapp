const { Resvg } = require('@resvg/resvg-js');
const fs = require('fs');
const path = require('path');

const icons = {
  inventory: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M20.54 5.23l-8-4a1 1 0 0 0-.97 0l-8 4A1 1 0 0 0 3 6.1v11.8a1 1 0 0 0 .54.88l8 4a1 1 0 0 0 .97 0l8-4a1 1 0 0 0 .54-.88V6.1a1 1 0 0 0-.51-.87zM12 3.2l5.6 2.8-5.6 2.8-5.6-2.8L12 3.2zm-7 14.1V8.8l6 3v8.5l-6-3.1zm14 0l-6 3.1v-8.5l6-3v8.4z"/></svg>`,
  purchase: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="21" r="2"/><circle cx="19" cy="21" r="2"/><path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/></svg>`,
  sales: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2H2v10l9.29 9.29c.94.94 2.48.94 3.42 0l6.58-6.58c.94-.94.94-2.48 0-3.42L12 2Z"/><path d="M7 7h.01"/></svg>`
};

const dir = 'src/static/tab';

function generate(name, isActive) {
    const color = isActive ? '#2979FF' : '#999999';
    let svg = icons[name];
    
    // Some visual tweaks: We padd 2px around the 24x24 viewBox to give breathing room in the 81x81 render.
    svg = svg.replace('viewBox="0 0 24 24"', 'viewBox="-2 -2 28 28"');

    if (svg.includes('currentColor')) {
        svg = svg.replace(/currentColor/g, color);
    }
    
    const resvg = new Resvg(svg, {
        fitTo: { mode: 'width', value: 81 },
        shapeRendering: 2
    });

    const pngData = resvg.render();
    const buffer = pngData.asPng();
    const filename = isActive ? name + '-active.png' : name + '.png';
    fs.writeFileSync(path.join(dir, filename), buffer);
    console.log('Generated ' + filename);
}

['inventory', 'purchase', 'sales'].forEach(n => { generate(n, false); generate(n, true); });
