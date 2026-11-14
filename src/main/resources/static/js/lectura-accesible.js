
(() => {
  'use strict';

  // ==========================================
  // CIUDADCONECTA PC8
  // LECTOR DE VOZ ACCESIBLE
  // ==========================================

  const sintetizador =
    ('speechSynthesis' in window &&
     'SpeechSynthesisUtterance' in window)
      ? window.speechSynthesis
      : null;

  const $ = id => document.getElementById(id);

  const selectorBloques =
    'h1,h2,h3,h4,h5,h6,p,li,td,th,' +
    'legend,summary,label,button,a';

  const selectorExcluir =
    'nav,header,footer,' +
    '.pc8-lector,.pc8-a11y-toolbar,' +
    '[hidden],[inert],[aria-hidden="true"],' +
    '.d-none,script,style,noscript,template';

  let voces = [];
  let fragmentos = [];
  let indice = 0;
  let sesion = 0;

  let leyendo = false;
  let pausado = false;
  let ultimoFoco = null;

  const ui = {};

  // ==========================================
  // MENSAJES DE ESTADO
  // ==========================================

  function informar(mensaje) {
    if (ui.estado) {
      ui.estado.textContent = mensaje;
    }
  }

  // ==========================================
  // HABILITAR O DESHABILITAR BOTONES
  // ==========================================

  function actualizarBotones() {
    if (ui.pausar) {
      ui.pausar.disabled = !leyendo || pausado;
    }

    if (ui.reanudar) {
      ui.reanudar.disabled = !leyendo || !pausado;
    }

    if (ui.detener) {
      ui.detener.disabled = !leyendo;
    }
  }

  // ==========================================
  // OBTENER CONTENIDO PRINCIPAL
  // ==========================================

  function principal() {
    return $('contenido') ||
      document.querySelector('main');
  }

  function normalizar(texto) {
    return String(texto || '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  // ==========================================
  // COMPROBAR VISIBILIDAD
  // ==========================================

  function visible(elemento) {
    if (elemento.closest(selectorExcluir)) {
      return false;
    }

    let actual = elemento;

    while (actual && actual.nodeType === 1) {
      const estilo = getComputedStyle(actual);

      if (
        estilo.display === 'none' ||
        estilo.visibility === 'hidden'
      ) {
        return false;
      }

      actual = actual.parentElement;
    }

    return true;
  }

  // ==========================================
  // EXTRAER BLOQUES PARA LECTURA
  // ==========================================

  function obtenerBloques() {
    const raiz = principal();

    if (!raiz) {
      return [];
    }

    return Array.from(
      raiz.querySelectorAll(selectorBloques)
    )
      .filter(elemento => visible(elemento))

      // Evitar leer dos veces el texto de
      // bloques que contienen otros bloques.
      .filter(elemento => {
        const padre =
          elemento.parentElement?.closest(
            selectorBloques
          );

        return !padre || !raiz.contains(padre);
      })

      .map(elemento => ({
        elemento,
        texto: normalizar(
          elemento.innerText ||
          elemento.textContent
        )
      }))

      .filter(bloque => bloque.texto.length > 0);
  }

  // ==========================================
  // DIVIDIR EL TEXTO
  // ==========================================

  function dividir(texto, limite = 180) {
    const resultado = [];
    let parte = '';

    for (const palabra of texto.split(/\s+/)) {
      if (
        parte &&
        parte.length + palabra.length + 1 > limite
      ) {
        resultado.push(parte);
        parte = '';
      }

      parte += (parte ? ' ' : '') + palabra;
    }

    if (parte) {
      resultado.push(parte);
    }

    return resultado;
  }

  // ==========================================
  // DETENER LECTURA
  // ==========================================

  function detener(mensaje = 'Lectura detenida.') {
    sesion++;

    if (sintetizador) {
      sintetizador.cancel();
    }

    leyendo = false;
    pausado = false;

    fragmentos = [];
    indice = 0;

    actualizarBotones();
    informar(mensaje);
  }

  // ==========================================
  // SELECCIONAR VOZ
  // ==========================================

  function vozElegida() {
    if (ui.voz && ui.voz.value !== '') {
      return voces[Number(ui.voz.value)] || null;
    }

    return voces.find(voz =>
      (voz.lang || '')
        .toLowerCase()
        .startsWith('es')
    ) || null;
  }

  // ==========================================
  // LEER SIGUIENTE FRAGMENTO
  // ==========================================

  function siguiente(token) {
    if (
      !sintetizador ||
      token !== sesion ||
      !leyendo ||
      pausado
    ) {
      return;
    }

    if (indice >= fragmentos.length) {
      leyendo = false;
      actualizarBotones();
      informar('Lectura finalizada.');
      return;
    }

    const mensaje = new SpeechSynthesisUtterance(
      fragmentos[indice]
    );

    const voz = vozElegida();

    if (voz) {
      mensaje.voice = voz;
    }

    mensaje.lang = voz ? voz.lang : 'es-PE';

    mensaje.rate = ui.velocidad
      ? Number(ui.velocidad.value)
      : 1;

    mensaje.pitch = ui.tono
      ? Number(ui.tono.value)
      : 1;

    mensaje.onend = () => {
      if (token !== sesion) {
        return;
      }

      indice++;
      siguiente(token);
    };

    mensaje.onerror = evento => {
      if (token !== sesion) {
        return;
      }

      if (
        evento.error === 'canceled' ||
        evento.error === 'interrupted'
      ) {
        return;
      }

      detener(
        'No se pudo continuar la lectura en este navegador.'
      );
    };

    sintetizador.speak(mensaje);
  }

  // ==========================================
  // INICIAR LECTURA
  // ==========================================

  function iniciarDesde(bloques, desde) {
    if (!sintetizador) {
      informar(
        'Este navegador no admite lectura por voz.'
      );
      return;
    }

    const texto = bloques
      .slice(desde)
      .map(bloque => bloque.texto)
      .join('. ');

    if (!texto.trim()) {
      informar(
        'No se encontró texto visible para leer.'
      );
      return;
    }

    detener('');

    fragmentos = dividir(texto);
    indice = 0;

    leyendo = true;
    pausado = false;

    actualizarBotones();
    informar('Lectura iniciada.');

    siguiente(sesion);
  }

  // ==========================================
  // LEER PÁGINA
  // ==========================================

  function leerPagina() {
    iniciarDesde(obtenerBloques(), 0);
  }

  // ==========================================
  // LEER DESDE EL FOCO
  // ==========================================

  function leerDesdeFoco() {
    const bloques = obtenerBloques();
    const raiz = principal();

    const foco = document.activeElement;

    const referencia =
      foco && raiz && raiz.contains(foco)
        ? foco
        : ultimoFoco;

    if (
      !referencia ||
      !raiz ||
      !raiz.contains(referencia)
    ) {
      informar(
        'Sin posición identificada. Lectura desde el inicio.'
      );

      iniciarDesde(bloques, 0);
      return;
    }

    const posicion = bloques.findIndex(
      ({ elemento }) =>
        elemento === referencia ||
        elemento.contains(referencia) ||
        Boolean(
          referencia.compareDocumentPosition(
            elemento
          ) & Node.DOCUMENT_POSITION_FOLLOWING
        )
    );

    iniciarDesde(
      bloques,
      posicion >= 0 ? posicion : 0
    );
  }

  // ==========================================
  // CARGAR VOCES DISPONIBLES
  // ==========================================

  function cargarVoces() {
    if (!sintetizador || !ui.voz) {
      return;
    }

    const disponibles = sintetizador.getVoices();

    if (!disponibles.length) {
      voces = [];
      informar('No se detectaron voces. Comprueba la configuración de voz del navegador o del sistema.');
      return;
    }

    informar('Voces detectadas. Puedes iniciar la lectura.');

    const anterior = ui.voz.value;

    voces = disponibles;

    ui.voz.replaceChildren(
      new Option('Automática (español)', '')
    );

    voces
      .map((voz, indice) => ({ voz, indice }))

      .sort((a, b) =>
        Number(
          (b.voz.lang || '')
            .toLowerCase()
            .startsWith('es')
        ) -
        Number(
          (a.voz.lang || '')
            .toLowerCase()
            .startsWith('es')
        )
      )

      .forEach(({ voz, indice }) => {
        ui.voz.add(
          new Option(
            `${voz.name} (${voz.lang})`,
            String(indice)
          )
        );
      });

    ui.voz.value =
      anterior !== '' && voces[Number(anterior)]
        ? anterior
        : '';
  }

  // ==========================================
  // INICIALIZAR CONTROLES
  // ==========================================

  function inicializar() {
    Object.assign(ui, {
      leer: $('pc8-leer-pagina'),

      desdeFoco: $('pc8-leer-desde-foco'),

      pausar: $('pc8-lectura-pausar'),

      reanudar: $('pc8-lectura-reanudar'),

      detener: $('pc8-lectura-detener'),

      voz: $('pc8-lectura-voz'),

      velocidad: $('pc8-lectura-velocidad'),

      tono: $('pc8-lectura-tono'),

      velocidadValor: $('pc8-velocidad-valor'),

      tonoValor: $('pc8-tono-valor'),

      estado: $('pc8-lectura-estado')
    });

    if (!ui.leer) {
      return;
    }

    // Comprobar compatibilidad.

    if (!sintetizador) {
      [
        ui.leer,
        ui.desdeFoco,
        ui.pausar,
        ui.reanudar,
        ui.detener,
        ui.voz,
        ui.velocidad,
        ui.tono
      ].forEach(control => {
        if (control) {
          control.disabled = true;
        }
      });

      informar(
        'La lectura por voz no está disponible en este navegador.'
      );

      return;
    }

    // ======================================
    // RECORDAR ÚLTIMO ELEMENTO ENFOCADO
    // ======================================

    document.addEventListener(
      'focusin',
      evento => {
        if (
          principal()?.contains(evento.target) &&
          !evento.target.closest(
            '.pc8-lector,.pc8-a11y-toolbar'
          )
        ) {
          ultimoFoco = evento.target;
        }
      }
    );

    // ======================================
    // BOTONES DE LECTURA
    // ======================================

    ui.leer.addEventListener(
      'click',
      leerPagina
    );

    ui.desdeFoco?.addEventListener(
      'click',
      leerDesdeFoco
    );

    // ======================================
    // PAUSAR
    // ======================================

    ui.pausar?.addEventListener('click', () => {
      if (!leyendo || pausado) {
        return;
      }

      sintetizador.pause();
      pausado = true;

      actualizarBotones();
      informar('Lectura pausada.');
    });

    // ======================================
    // REANUDAR
    // ======================================

    ui.reanudar?.addEventListener('click', () => {
      if (!leyendo || !pausado) {
        return;
      }

      pausado = false;
      sintetizador.resume();

      actualizarBotones();
      informar('Lectura reanudada.');
    });

    // ======================================
    // DETENER
    // ======================================

    ui.detener?.addEventListener(
      'click',
      () => detener()
    );

    // ======================================
    // VELOCIDAD
    // ======================================

    ui.velocidad?.addEventListener(
      'input',
      () => {
        if (ui.velocidadValor) {
          ui.velocidadValor.textContent =
            Number(ui.velocidad.value)
              .toFixed(1) + '×';
        }
      }
    );

    // ======================================
    // TONO
    // ======================================

    ui.tono?.addEventListener(
      'input',
      () => {
        if (ui.tonoValor) {
          ui.tonoValor.textContent =
            Number(ui.tono.value)
              .toFixed(1);
        }
      }
    );

    // ======================================
    // VOCES Y ESTADO INICIAL
    // ======================================

    cargarVoces();

    if (sintetizador.addEventListener) {
      sintetizador.addEventListener(
        'voiceschanged',
        cargarVoces
      );
    } else {
      sintetizador.onvoiceschanged = cargarVoces;
    }

    actualizarBotones();

    if (!voces.length) {
      informar('El navegador admite lectura por voz, pero no detecta voces instaladas.');
    }

    // Detener cuando se abandona la página.

    window.addEventListener(
      'pagehide',
      () => detener('')
    );
  }

  // ==========================================
  // EJECUTAR CUANDO CARGUE EL DOCUMENTO
  // ==========================================

  if (document.readyState === 'loading') {
    document.addEventListener(
      'DOMContentLoaded',
      inicializar
    );
  } else {
    inicializar();
  }

})();
