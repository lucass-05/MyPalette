// arreglo para safari 27.2 (y todos los navegadores de ios 27.2): la web se queda en blanco sin ningun error.
//
// webkit estropea los numeros con decimales que pasan de javascript a webassembly cuando la llamada
// es la ultima de la funcion (una "llamada de cola"): llegan como NaN, compose mide y pinta con
// tamaños sin sentido y no se ve nada. es el fallo 329421 de webkit (CMP-10986 en compose), ya
// arreglado en webkit pero no en esa version de ios.
//
// cada funcion que skiko exporta es justo eso: (...a) => (f = loadedWasm._["f"])(...a).
// aqui se reescriben para que la llamada vaya dentro de un try, y una llamada dentro de un try nunca
// es de cola. si falla, el error se vuelve a lanzar igual que antes.
//
// kotlin junta todos los archivos de webpack.config.d con la configuracion que genera. este crea el
// "loader" (el paso de webpack que reescribe skiko.mjs) junto a esa configuracion y lo engancha.
// borrar cuando las versiones de ios que se usen ya traigan el arreglo de webkit.
;(function () {
    const fs = require("fs");
    const path = require("path");
    const loader = path.resolve(__dirname, "skiko-sin-llamadas-de-cola.cjs");
    fs.writeFileSync(loader, [
        "module.exports = function (codigo) {",
        "    let cambiadas = 0;",
        "    const nuevo = codigo.replace(/\\(\\.\\.\\.(\\w+)\\) => \\(([^()]+?)\\)\\(\\.\\.\\.\\1\\)/g, function (_, a, llamada) {",
        "        cambiadas++;",
        "        return '(...' + a + ') => { try { return (' + llamada + ')(...' + a + '); } catch (e) { throw e; } }';",
        "    });",
        "    console.log('palette: ' + cambiadas + ' llamadas de cola de skiko cambiadas en ' + this.resourcePath);",
        "    return nuevo;",
        "};",
    ].join("\n"));
    config.module = config.module || {};
    config.module.rules = config.module.rules || [];
    config.module.rules.push({ test: /skiko\.m?js$/, use: [loader] });
})();
