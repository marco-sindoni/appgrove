// Contenuti marketing — spagnolo (UC 0037). Tono lean, job-led + privacy come firma.
import type { MarketingContent } from './types.ts'

export const es: MarketingContent = {
  nav: {
    app: 'Aplicaciones',
    why: 'Por qué appgrove',
    pricing: 'Precios',
    login: 'Iniciar sesión',
    signup: 'Registrarse',
  },
  hero: {
    badge: 'todo en Europa · GDPR primero',
    title: 'Herramientas sencillas que crecen con tu negocio',
    subtitle:
      'Un ecosistema de aplicaciones rápidas y asequibles para pequeñas y medianas empresas. Menos tiempo en la gestión y más tiempo para lo que mejor sabes hacer.',
    ctaPrimary: 'Empezar',
    ctaSecondary: 'Descubrir las aplicaciones',
  },
  apps: {
    title: 'Una cuenta, todas las herramientas que necesitas',
    subtitle: 'Aplicaciones enfocadas que hacen bien una sola cosa — sin suites recargadas, sin ataduras.',
    comingSoon: 'Próximamente',
    flagshipName: 'Facturación',
    flagshipCategory: 'Facturas y cobros',
    flagshipDescription:
      'Crea y envía facturas conformes en minutos, controla los pagos y mantén tus cuentas al día.',
    moreTitle: 'Más herramientas en camino',
    moreText:
      'El catálogo es pequeño a propósito, y va creciendo. Con regularidad se suman nuevas aplicaciones al ecosistema — y una sola cuenta las desbloquea todas.',
  },
  crossSell: {
    title: 'Una cuenta, muchas herramientas',
    body: 'A medida que el ecosistema crece, tu cuenta crece con él. Añade una nueva herramienta cuando la necesites — el mismo acceso, el mismo hogar de confianza para tus datos.',
    points: [
      'Un único acceso para todas las aplicaciones',
      'Una experiencia sencilla y coherente entre herramientas',
      'Añade herramientas a medida que tu negocio crece',
    ],
  },
  ai: {
    title: 'Listas para la IA',
    body: 'Cada aplicación de appgrove está diseñada para que tu asistente de IA pueda invocarla. Pídelo en lenguaje natural en ChatGPT, Claude o Perplexity — y deja que resuelva la tarea dentro de tu aplicación.',
    points: [
      'Construidas sobre MCP (Model Context Protocol), el estándar abierto con el que los asistentes de IA invocan herramientas externas',
      'Haz el trabajo directamente desde el chat que ya usas',
      'Tus herramientas, accesibles allí donde ya trabajas',
    ],
    note: 'Así es como diseñamos el ecosistema — estamos avanzando hacia ello hoy, aplicación a aplicación.',
  },
  privacy: {
    title: 'Alojado en la UE. Derechos GDPR completos.',
    body: 'Tus datos viven en Europa, bajo la ley europea. Aquí la privacidad no es un añadido: es la forma en que appgrove está construido.',
    points: [
      'Todos los datos alojados en la UE',
      'Derechos GDPR completos, por diseño',
      'Sin rastreadores ocultos, sin venta de datos',
    ],
  },
  newsletter: {
    title: 'Mantente al día',
    body: 'Te avisamos cuando lleguen nuevas aplicaciones. Sin spam, y puedes darte de baja cuando quieras.',
    placeholder: 'tu@email.com',
    cta: 'Avísame',
    note: 'Solo te escribiremos sobre appgrove. Las suscripciones abren pronto.',
  },
  finalCta: {
    title: '¿Listo para empezar?',
    body: 'Crea tu cuenta y explora el ecosistema — pruébalo gratis.',
    primary: 'Empezar',
    secondary: 'Por qué appgrove',
  },
  why: {
    title: 'Por qué appgrove',
    intro:
      'Creamos herramientas sencillas y enfocadas para pequeñas y medianas empresas en Europa. Cuatro promesas guían todo lo que hacemos.',
    sections: [
      {
        title: 'Herramientas que te devuelven tiempo',
        body: 'Las pequeñas empresas no necesitan suites recargadas. Necesitan herramientas que hagan bien una cosa, rápido y a un precio justo. Cada aplicación de appgrove está pensada para quitarte la gestión de encima y que te centres en tu negocio.',
      },
      {
        title: 'Un ecosistema en crecimiento',
        body: 'Una cuenta, muchas herramientas. Cuando tus necesidades crecen, el ecosistema crece contigo — añade una nueva aplicación cuando la necesites, sin nuevos accesos ni nuevos silos.',
      },
      {
        title: 'Listas para la IA',
        body: 'Diseñamos cada aplicación para que tu asistente de IA pueda invocarla, a través de MCP — el estándar abierto con el que los asistentes de IA invocan herramientas externas. La visión: pides en lenguaje natural y el trabajo se hace dentro de tu aplicación. Estamos avanzando hacia ello hoy, aplicación a aplicación.',
      },
      {
        title: 'Europea por diseño, privada por defecto',
        body: 'Tus datos están alojados en la UE, bajo la ley europea, con derechos GDPR completos. La privacidad es la base, no una función — sin rastreadores ocultos, y tus datos nunca se venden.',
      },
    ],
  },
  pricing: {
    title: 'Precios — cómo funciona la facturación',
    intro:
      'Cada aplicación fija su propio precio en su página. Así funciona la facturación en appgrove.',
    sections: [
      {
        title: 'Precio por aplicación',
        body: 'Pagas solo por las herramientas que usas. Cada aplicación muestra sus planes y precios en su propia página — elige lo que encaja con tu negocio.',
      },
      {
        title: 'Mensual o anual',
        body: 'Elige el ciclo de facturación que prefieras. El anual es el predeterminado y sale más barato al año; el mensual te da flexibilidad.',
      },
      {
        title: 'Prueba gratuita de 14 días',
        body: 'Prueba antes de pagar. Cada plan de pago empieza con una prueba gratuita de 14 días — no se cobra nada hasta que termina.',
      },
      {
        title: 'Sin reembolsos',
        body: 'Como cada plan empieza con una prueba gratuita, las suscripciones no son reembolsables una vez facturadas. Los detalles están en nuestra política de reembolsos.',
      },
    ],
    refundLinkText: 'Leer la política de reembolsos',
  },
  footer: {
    tagline: 'Herramientas sencillas que crecen con tu negocio.',
    legalHeading: 'Legal',
    supportLabel: 'Soporte',
    securityLabel: 'Seguridad',
    newsletterTitle: 'Newsletter',
    newsletterBody: 'Las nuevas aplicaciones, en tu bandeja de entrada.',
    newsletterPlaceholder: 'tu@email.com',
    newsletterCta: 'Avísame',
    rights: 'appgrove — todo en Europa, GDPR primero.',
  },
}
