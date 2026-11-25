// arte propio para la pagina de presentacion: portadas, fotos de perfil y portadas de perfil dibujadas
// con svg desde aqui mismo. los titulos son inventados, asi que no hay derechos de nadie de por medio
// y no hay imagenes que cargar: todo sale al momento.
(function () {
    // numeros "aleatorios" que salen siempre iguales para la misma semilla
    function azar(semilla) {
        let a = semilla >>> 0;
        return function () {
            a |= 0; a = (a + 0x6d2b79f5) | 0;
            let t = Math.imul(a ^ (a >>> 15), 1 | a);
            t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        };
    }

    let cuenta = 0;
    const id = (p) => p + (cuenta++);

    // una sierra de montañas de ancho w, entre las alturas y0 e y1
    function montes(r, w, h, y0, y1, puntas) {
        let d = `M0 ${h} L0 ${y0 + r() * (y1 - y0)}`;
        for (let i = 1; i <= puntas; i++) d += ` L${(w * i) / puntas} ${y0 + r() * (y1 - y0)}`;
        return d + ` L${w} ${h} Z`;
    }

    function olas(r, w, y, amp, fase) {
        let d = `M0 ${y}`;
        for (let x = 0; x <= w; x += 10) d += ` L${x} ${y + Math.sin(x / 22 + fase) * amp}`;
        return d + ` L${w} 400 L0 400 Z`;
    }

    // el dibujo de fondo de una portada, segun su motivo
    function fondo(motivo, c, r, w, h) {
        const g = id('g');
        let s = `<defs><linearGradient id="${g}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${c[0]}"/><stop offset="1" stop-color="${c[1]}"/></linearGradient></defs>`;
        s += `<rect width="${w}" height="${h}" fill="url(#${g})"/>`;
        if (motivo === 'sol') {
            s += `<circle cx="${w * (0.35 + r() * 0.3)}" cy="${h * 0.42}" r="${w * 0.24}" fill="${c[2]}"/>`;
            s += `<path d="${montes(r, w, h, h * 0.5, h * 0.66, 5)}" fill="${c[3]}" opacity=".85"/>`;
            s += `<path d="${montes(r, w, h, h * 0.62, h * 0.76, 4)}" fill="${c[4]}"/>`;
        } else if (motivo === 'olas') {
            s += `<circle cx="${w * 0.7}" cy="${h * 0.25}" r="${w * 0.12}" fill="${c[2]}" opacity=".9"/>`;
            [0.5, 0.6, 0.7, 0.8].forEach((y, i) => { s += `<path d="${olas(r, w, h * y, 6 + i * 2, i * 1.7 + r())}" fill="${c[3 + (i % 2)]}" opacity="${0.55 + i * 0.12}"/>`; });
        } else if (motivo === 'ciudad') {
            for (let i = 0; i < 40; i++) s += `<circle cx="${r() * w}" cy="${r() * h * 0.5}" r="${r() * 1.2 + 0.3}" fill="#fff" opacity="${0.4 + r() * 0.6}"/>`;
            s += `<circle cx="${w * 0.75}" cy="${h * 0.2}" r="${w * 0.08}" fill="${c[2]}"/>`;
            let x = 0;
            while (x < w) {
                const bw = 14 + r() * 26, bh = h * (0.25 + r() * 0.35);
                s += `<rect x="${x}" y="${h - bh}" width="${bw}" height="${bh}" fill="${c[3]}"/>`;
                for (let wy = h - bh + 8; wy < h - 8; wy += 9) for (let wx = x + 4; wx < x + bw - 4; wx += 7) if (r() > 0.55) s += `<rect x="${wx}" y="${wy}" width="3" height="4" fill="${c[2]}" opacity=".85"/>`;
                x += bw + 2;
            }
        } else if (motivo === 'espacio') {
            for (let i = 0; i < 70; i++) s += `<circle cx="${r() * w}" cy="${r() * h}" r="${r() * 1.3 + 0.2}" fill="#fff" opacity="${0.3 + r() * 0.7}"/>`;
            const cx = w * 0.55, cy = h * 0.42, pr = w * 0.26;
            const pg = id('p');
            s += `<defs><radialGradient id="${pg}" cx=".35" cy=".35"><stop offset="0" stop-color="${c[2]}"/><stop offset="1" stop-color="${c[3]}"/></radialGradient></defs>`;
            s += `<circle cx="${cx}" cy="${cy}" r="${pr}" fill="url(#${pg})"/>`;
            s += `<ellipse cx="${cx}" cy="${cy}" rx="${pr * 1.7}" ry="${pr * 0.35}" fill="none" stroke="${c[4]}" stroke-width="3" transform="rotate(-18 ${cx} ${cy})"/>`;
        } else if (motivo === 'bosque') {
            s += `<circle cx="${w * 0.3}" cy="${h * 0.3}" r="${w * 0.1}" fill="${c[2]}"/>`;
            [0.62, 0.74, 0.86].forEach((y, k) => {
                for (let x = -10; x < w + 10; x += 16 + r() * 10) {
                    const th = 30 + r() * 40 + k * 10;
                    s += `<path d="M${x} ${h * y + 20} L${x + 9} ${h * y + 20 - th} L${x + 18} ${h * y + 20} Z" fill="${c[3 + (k % 2)]}" opacity="${0.6 + k * 0.2}"/>`;
                }
            });
            s += `<rect y="${h * 0.9}" width="${w}" height="${h * 0.1}" fill="${c[4]}"/>`;
        } else if (motivo === 'bauhaus') {
            s += `<circle cx="${w * 0.32}" cy="${h * 0.34}" r="${w * 0.24}" fill="${c[2]}"/>`;
            s += `<rect x="${w * 0.5}" y="${h * 0.22}" width="${w * 0.36}" height="${w * 0.36}" fill="${c[3]}" transform="rotate(${r() * 20 - 10} ${w * 0.68} ${h * 0.34})"/>`;
            s += `<path d="M${w * 0.15} ${h * 0.72} L${w * 0.5} ${h * 0.42} L${w * 0.85} ${h * 0.72} Z" fill="${c[4]}" opacity=".9"/>`;
        } else if (motivo === 'aros') {
            const cx = w * (0.4 + r() * 0.2), cy = h * (0.4 + r() * 0.2);
            for (let i = 7; i > 0; i--) s += `<circle cx="${cx}" cy="${cy}" r="${(w * 0.08) * i}" fill="${c[2 + (i % 3)]}" opacity="${0.35 + (7 - i) * 0.08}"/>`;
        } else if (motivo === 'pixel') {
            const t = 10;
            for (let y = 0; y < h; y += t) for (let x = 0; x < w; x += t) {
                const alto = h * 0.55 + Math.sin(x / 30) * 20 + Math.sin(x / 11) * 8;
                if (y > alto) s += `<rect x="${x}" y="${y}" width="${t}" height="${t}" fill="${y > alto + 30 ? c[4] : c[3]}"/>`;
            }
            s += `<rect x="${w * 0.68}" y="${h * 0.16}" width="${t * 3}" height="${t * 3}" fill="${c[2]}"/>`;
        }
        return s;
    }

    const esc = (t) => String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;');

    // portada completa: el dibujo y, encima, el titulo como en un cartel de verdad
    function portada({semilla, titulo, autor, motivo, colores, forma = 'poster'}) {
        const r = azar(semilla);
        const w = 200, h = forma === 'cuadrada' ? 200 : 300;
        let s = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid slice">`;
        s += fondo(motivo, colores, r, w, h);
        if (forma === 'cuadrada') {
            s += `<text x="14" y="${h - 16}" font-family="Young Serif, Georgia, serif" font-size="20" fill="#fff" opacity=".95">${esc(titulo)}</text>`;
        } else {
            s += `<rect y="${h - 78}" width="${w}" height="78" fill="#000" opacity=".18"/>`;
            s += `<text x="${w / 2}" y="${h - 44}" text-anchor="middle" font-family="Young Serif, Georgia, serif" font-size="${titulo.length > 14 ? 17 : 22}" fill="#fff">${esc(titulo)}</text>`;
            if (autor) s += `<text x="${w / 2}" y="${h - 22}" text-anchor="middle" font-family="-apple-system, Segoe UI, Roboto, sans-serif" font-size="9" letter-spacing="2" fill="#fff" opacity=".85">${esc(autor.toUpperCase())}</text>`;
        }
        return s + '</svg>';
    }

    // portada de perfil (banner): un paisaje ancho
    function banner({semilla, motivo, colores}) {
        const r = azar(semilla);
        const w = 600, h = 220;
        return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid slice">${fondo(motivo, colores, r, w, h)}</svg>`;
    }

    // foto de perfil ilustrada: fondo, hombros, cara y pelo
    function retrato({semilla, piel, pelo, ropa, fondo: color, peinado = 'corto', gafas = false}) {
        const r = azar(semilla);
        let s = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="${color}"/>`;
        if (peinado === 'largo') s += `<path d="M27 46 Q26 20 50 18 Q74 20 73 46 L76 84 L24 84 Z" fill="${pelo}"/>`;
        s += `<path d="M16 100 Q18 74 50 72 Q82 74 84 100 Z" fill="${ropa}"/>`;
        s += `<rect x="43" y="58" width="14" height="16" rx="6" fill="${piel}"/>`;
        s += `<ellipse cx="50" cy="45" rx="17" ry="20" fill="${piel}"/>`;
        if (peinado === 'corto') s += `<path d="M32 42 Q31 22 50 22 Q70 22 68 42 Q64 32 50 31 Q38 31 32 42 Z" fill="${pelo}"/>`;
        if (peinado === 'largo') s += `<path d="M33 40 Q36 24 50 24 Q65 24 67 40 Q60 30 50 31 Q41 31 33 40 Z" fill="${pelo}"/>`;
        if (peinado === 'rizado') for (let i = 0; i < 14; i++) s += `<circle cx="${33 + (i % 7) * 5.6}" cy="${26 + Math.floor(i / 7) * 6 + r() * 2}" r="6.5" fill="${pelo}"/>`;
        if (peinado === 'mono') s += `<circle cx="50" cy="18" r="9" fill="${pelo}"/><path d="M33 40 Q34 25 50 25 Q66 25 67 40 Q60 31 50 32 Q40 31 33 40 Z" fill="${pelo}"/>`;
        s += `<circle cx="44" cy="47" r="1.6" fill="#2b2b2b"/><circle cx="56" cy="47" r="1.6" fill="#2b2b2b"/>`;
        s += `<path d="M45 55 Q50 59 55 55" stroke="#2b2b2b" stroke-width="1.4" fill="none" stroke-linecap="round"/>`;
        if (gafas) s += `<g fill="none" stroke="#1f1f1f" stroke-width="1.6"><circle cx="44" cy="47" r="5"/><circle cx="56" cy="47" r="5"/><path d="M49 47 L51 47"/></g>`;
        return s + '</svg>';
    }

    // los contenidos que salen en la pagina, todos inventados
    const P = (t) => t;
    window.paletteArte = {
        portadas: {
            marea: {semilla: 11, titulo: 'Marea alta', autor: 'Inés Varela', motivo: 'olas', colores: ['#ffb38a', '#2d5bf0', '#fff3c4', '#1e3a8a', '#3b82f6']},
            verano: {semilla: 7, titulo: 'El último verano', autor: 'Pau Ribas', motivo: 'sol', colores: ['#ffd27a', '#f2473a', '#fff1c1', '#a5281c', '#5c1a12']},
            ciudad: {semilla: 3, titulo: 'Turno de noche', autor: 'Serie', motivo: 'ciudad', colores: ['#0b1026', '#3b2a6e', '#ffd166', '#111827', '#0f172a']},
            orbita: {semilla: 21, titulo: 'Órbita', autor: 'Elena Soto', motivo: 'espacio', colores: ['#05060f', '#1b1145', '#c4b5fd', '#5b21b6', '#e9d5ff']},
            raices: {semilla: 5, titulo: 'Raíces', autor: 'Marta Llorente', motivo: 'bosque', colores: ['#dff3e4', '#9ad1b0', '#fde68a', '#166534', '#14532d']},
            jardin: {semilla: 9, titulo: 'El jardín de cristal', autor: 'Lía Montero', motivo: 'bauhaus', colores: ['#fff7ed', '#fde2e4', '#e8418f', '#2d5bf0', '#ffb81f']},
            horas: {semilla: 31, titulo: 'Las horas azules', autor: 'Andrés Gil', motivo: 'olas', colores: ['#e0f2fe', '#1d4ed8', '#ffffff', '#1e40af', '#60a5fa']},
            senal: {semilla: 44, titulo: 'Señal', autor: 'Serie', motivo: 'aros', colores: ['#111827', '#7c2d12', '#f97316', '#fb923c', '#fdba74']},
            norte: {semilla: 13, titulo: 'Norte', autor: 'Ana Ruiz', motivo: 'sol', colores: ['#e0e7ff', '#6366f1', '#ffffff', '#3730a3', '#1e1b4b']},
            valle: {semilla: 17, titulo: 'Valle eterno', autor: 'Estudio Faro', motivo: 'pixel', colores: ['#bae6fd', '#e0f2fe', '#fde047', '#16a34a', '#14532d']},
            faro: {semilla: 27, titulo: 'Faro', autor: 'Estudio Brisa', motivo: 'olas', colores: ['#1e293b', '#0f766e', '#fef08a', '#134e4a', '#14b8a6']},
            agosto: {semilla: 2, titulo: 'Luz de agosto', motivo: 'aros', colores: ['#fff7ed', '#ffb81f', '#f2473a', '#e8418f', '#ffb81f'], forma: 'cuadrada'},
            neon: {semilla: 8, titulo: 'Neón', motivo: 'aros', colores: ['#0f0a1e', '#3b0764', '#e879f9', '#8b4bdb', '#22d3ee'], forma: 'cuadrada'},
            ola: {semilla: 19, titulo: 'Ola', motivo: 'olas', colores: ['#ccfbf1', '#14b8a6', '#ffffff', '#0f766e', '#5eead4'], forma: 'cuadrada'},
            dormida: {semilla: 41, titulo: 'La ciudad dormida', autor: 'Clara Mir', motivo: 'ciudad', colores: ['#1e1b4b', '#be185d', '#fbcfe8', '#2e1065', '#1e1b4b']},
            olmos: {semilla: 23, titulo: 'Los Olmos', autor: 'Serie', motivo: 'bosque', colores: ['#fef3c7', '#f59e0b', '#fff7ed', '#92400e', '#451a03']},
        },
        portada, banner, retrato,
    };

    // pinta cada elemento con data-arte="portada:clave", "banner:..." o "retrato:..."
    function pintar() {
        const A = window.paletteArte;
        document.querySelectorAll('[data-arte]').forEach((el) => {
            const [tipo, clave] = el.dataset.arte.split(':');
            if (tipo === 'portada') el.innerHTML = portada(A.portadas[clave]);
            if (tipo === 'banner') el.innerHTML = banner(A.banners[clave]);
            if (tipo === 'retrato') el.innerHTML = retrato(A.retratos[clave]);
        });
    }
    window.paletteArte.banners = {
        lucia: {semilla: 4, motivo: 'sol', colores: ['#fbcfe8', '#8b4bdb', '#fff1c1', '#5b21b6', '#2e1065']},
        dani: {semilla: 6, motivo: 'olas', colores: ['#fed7aa', '#c2410c', '#fff7ed', '#7c2d12', '#9a3412']},
        marta: {semilla: 12, motivo: 'espacio', colores: ['#020617', '#1e1b4b', '#93c5fd', '#1d4ed8', '#c4b5fd']},
        alba: {semilla: 15, motivo: 'bosque', colores: ['#ffe4ef', '#f9a8d4', '#fff1f2', '#be185d', '#831843']},
    };
    window.paletteArte.retratos = {
        lucia: {semilla: 1, piel: '#f1c7a5', pelo: '#3b2314', ropa: '#2d5bf0', fondo: '#ffe08a', peinado: 'largo'},
        dani: {semilla: 2, piel: '#d9a074', pelo: '#1f1a17', ropa: '#f2473a', fondo: '#ffd7c2', peinado: 'corto', gafas: true},
        marta: {semilla: 3, piel: '#f3d2b8', pelo: '#111111', ropa: '#8b4bdb', fondo: '#c7d2fe', peinado: 'mono'},
        alba: {semilla: 4, piel: '#8d5a3b', pelo: '#2a1a12', ropa: '#12a679', fondo: '#fbcfe8', peinado: 'rizado'},
        pablo: {semilla: 5, piel: '#e8b98f', pelo: '#a0522d', ropa: '#ffb81f', fondo: '#bae6fd', peinado: 'corto'},
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', pintar);
    else pintar();
})();
