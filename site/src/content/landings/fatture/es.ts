// Landing dell'app #1 "Fatture" (UC 0053) — spagnolo.
import type { LandingLocaleContent } from '../types.ts'

export const es: LandingLocaleContent = {
  slug: 'facturas',
  meta: {
    title: 'Fatture — facturación sin el papeleo, alojada en la UE',
    description:
      'Crea, envía y controla tus facturas en unos clics. Una aplicación de facturación sencilla y de un solo usuario — tus datos alojados en la UE, con todos los derechos del RGPD.',
    ogImage: '/landings/fatture/og.png',
  },
  hero: {
    badge: 'todo en la UE · RGPD primero',
    title: 'La facturación, sin el papeleo',
    subtitle:
      'Con Fatture creas, envías y controlas tus facturas en unos clics — una aplicación sencilla que hace bien una sola cosa, con tus datos alojados en la UE.',
    ctaPrimary: 'Empieza gratis',
    ctaSecondary: 'Descubre cómo funciona',
    screenshot: {
      src: '/landings/fatture/hero.es.png',
      alt: 'La lista de facturas de Fatture, con número, cliente, estado y total',
    },
  },
  problemSolution: {
    title: 'Menos tiempo en el papeleo',
    problem:
      'Para autónomos y pequeñas empresas, facturar significa plantillas incómodas, numeración manual y hojas de cálculo que nunca cuadran — tiempo que se le quita al trabajo de verdad.',
    solution:
      'Fatture lo reduce a unos clics: añade un cliente, enumera las líneas, y el total y el número progresivo se calculan por ti. Nada que instalar, sin estorbos.',
  },
  features: {
    title: 'Todo lo que necesita facturar, nada de más',
    subtitle: 'Una herramienta sencilla que hace bien un trabajo — sin suites recargadas, sin ataduras.',
    items: [
      {
        icon: 'receipt_long',
        title: 'Facturas en unos clics',
        body: 'Añade un cliente y las líneas — descripción, cantidad, precio unitario — y Fatture calcula el total por ti.',
      },
      {
        icon: 'tag',
        title: 'Numeración automática',
        body: 'Un número de factura progresivo por año, asignado por ti y nunca reutilizado — ordenado y coherente.',
      },
      {
        icon: 'fact_check',
        title: 'Estado siempre claro',
        body: 'Lleva cada factura de borrador a emitida, pagada o anulada, para saber siempre en qué punto estás.',
      },
      {
        icon: 'lock',
        title: 'Alojada en la UE',
        body: 'Tus facturas viven en la UE, bajo la ley europea, con todos los derechos del RGPD y sin rastreadores ocultos.',
      },
    ],
  },
  howItWorks: {
    title: 'Operativo en tres pasos',
    steps: [
      { title: 'Crea tu cuenta', body: 'Registro en segundos — sin tarjeta, el plan gratuito está listo para usar.' },
      { title: 'Añade tu primera factura', body: 'Introduce el cliente y las líneas; el total y el número de factura se rellenan por ti.' },
      { title: 'Síguela hasta pagada', body: 'Emite la factura y actualiza su estado a medida que pasa de borrador a pagada.' },
    ],
  },
  pricing: {
    title: 'Un solo plan gratuito, sin sorpresas',
    subtitle: 'Fatture es gratis. Crea hasta 10 facturas al mes — sin tarjeta, sin prueba que caduque.',
    monthlyLabel: 'Mensual',
    yearlyLabel: 'Anual',
    trialNote: 'Sin tarjeta, nunca. El plan gratuito es tuyo.',
    tiers: [
      {
        name: 'Gratis',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: [
          'Hasta 10 facturas al mes',
          'Clientes y líneas de factura',
          'Seguimiento del estado y numeración automática',
          'Datos alojados en la UE, todos los derechos del RGPD',
        ],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Alojada en la UE. Todos los derechos del RGPD.',
    body: 'Tus facturas se quedan en Europa, bajo la ley europea. Puedes exportar o borrar tus datos tú mismo, cuando quieras — y nunca se venden.',
    points: [
      'Todos los datos alojados en la UE',
      'Exporta o borra tus datos tú mismo (RGPD)',
      'Facturas conservadas conforme a las obligaciones fiscales',
      'Sin rastreadores ocultos, sin venta de datos',
    ],
  },
  faq: {
    title: 'Preguntas frecuentes',
    items: [
      {
        q: '¿Cuánto cuesta Fatture?',
        a: 'Es gratis. El plan actual te permite crear hasta 10 facturas al mes, sin tarjeta y sin prueba que caduque.',
      },
      {
        q: '¿Qué pasa cuando llego a 10 facturas en un mes?',
        a: 'El recuento mensual se reinicia al principio de cada mes natural, así que puedes crear nuevas facturas desde el mes siguiente.',
      },
      {
        q: '¿Dónde se guardan mis datos?',
        a: 'Enteramente en la Unión Europea, bajo la ley europea, con todos los derechos del RGPD.',
      },
      {
        q: '¿Puedo exportar o borrar mis datos?',
        a: 'Sí. Puedes exportar o borrar definitivamente tus datos tú mismo, desde tu cuenta, en cualquier momento.',
      },
    ],
  },
  finalCta: {
    title: '¿Listo para enviar tu primera factura?',
    body: 'Crea tu cuenta y empieza a facturar gratis — sin tarjeta.',
    primary: 'Empieza gratis',
    secondary: 'Por qué appgrove',
  },
}
