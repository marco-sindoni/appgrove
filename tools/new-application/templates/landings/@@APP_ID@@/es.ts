// Bozza landing di @@APP_NAME@@ — spagnolo. Copy generico on-brand: `finalize-landing`
// lo rifinisce. Il badge dell'hero porta il sentinella «DA RIFINIRE».
import type { LandingLocaleContent } from '../types.ts'

export const es: LandingLocaleContent = {
  slug: '@@LANDING_SLUG@@',
  meta: {
    title: '@@APP_NAME@@ — haz el trabajo, la privacidad primero',
    description:
      '@@APP_NAME@@ es una micro-app 100 % UE y RGPD-first de appgrove: hace bien una sola cosa, con tus datos a salvo en Europa.',
    ogImage: null,
  },
  hero: {
    badge: 'DA RIFINIRE — 100 % UE · RGPD-first',
    title: 'Haz el trabajo en minutos, no en toda una tarde',
    subtitle:
      '@@APP_NAME@@ te quita de encima una tarea recurrente — en pocos clics, con tus datos alojados en Europa y todos los derechos RGPD.',
    ctaPrimary: 'Empezar prueba gratis',
    ctaSecondary: 'Ver cómo funciona',
    screenshot: {
      src: null,
      alt: 'Captura del panel de @@APP_NAME@@',
    },
  },
  problemSolution: {
    title: 'Menos administración, más de lo que importa',
    problem:
      'Los equipos pequeños pierden horas en tareas administrativas repetitivas — repartidas entre herramientas que nunca acaban de entenderse.',
    solution:
      '@@APP_NAME@@ hace bien una sola cosa: te quita esa administración, en pocos clics, con tus datos a salvo en Europa.',
  },
  features: {
    title: 'Todo lo que necesitas, nada que no',
    subtitle: 'Funciones enfocadas que hacen bien una sola cosa — sin suite recargada, sin bloqueo.',
    items: [
      {
        icon: 'bolt',
        title: 'Rápida por defecto',
        body: 'Lista en minutos; las tareas de cada día se cierran en un par de clics.',
      },
      {
        icon: 'lock',
        title: 'Privada por diseño',
        body: 'Tus datos viven en la UE, bajo ley europea, con todos los derechos RGPD.',
      },
      {
        icon: 'sync',
        title: 'Una cuenta, todas las herramientas',
        body: 'Añade otras apps de appgrove cuando las necesites — mismo acceso, misma casa de confianza.',
      },
      {
        icon: 'smart_toy',
        title: 'Lista para la IA',
        body: 'Diseñada para que tu asistente de IA la alcance, y el trabajo se haga desde el chat que ya usas.',
      },
    ],
  },
  howItWorks: {
    title: 'En marcha en tres pasos',
    steps: [
      { title: 'Crea tu cuenta', body: 'Regístrate en segundos — sin tarjeta para empezar la prueba.' },
      { title: 'Prepara tu espacio', body: 'Una configuración guiada te hace productivo desde el primer día.' },
      { title: 'Haz el trabajo', body: 'Haz lo que tengas que hacer y deja que @@APP_NAME@@ aparte la administración.' },
    ],
  },
  pricing: {
    title: 'Precios simples y justos',
    subtitle: 'Elige el plan que encaja. El anual cuesta menos; el mensual da flexibilidad.',
    monthlyLabel: 'Mensual',
    yearlyLabel: 'Anual',
    trialNote: 'Cada plan de pago empieza con 14 días de prueba gratis — no se cobra nada hasta que termina.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: ['Para empezar', 'Funciones básicas', 'Soporte de la comunidad'],
      },
      {
        name: 'Pro',
        priceMonthly: '00 € / mes',
        priceYearly: '000 € / año',
        features: ['Todo lo de Starter', 'Todas las funciones', 'Soporte prioritario'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Alojada en la UE. Todos los derechos RGPD.',
    body: 'Aquí la privacidad no es un extra — es cómo está construido appgrove. Tus datos se quedan en Europa y nunca se venden.',
    points: ['Todos los datos alojados en la UE', 'Todos los derechos RGPD, por diseño', 'Sin rastreadores ocultos, sin venta de datos'],
  },
  faq: {
    title: 'Preguntas frecuentes',
    items: [
      {
        q: '¿Necesito tarjeta para probar @@APP_NAME@@?',
        a: 'No. La prueba gratis de 14 días empieza sin tarjeta; solo pagas si decides continuar.',
      },
      {
        q: '¿Dónde se guardan mis datos?',
        a: 'Enteramente en la Unión Europea, bajo ley europea, con todos los derechos RGPD.',
      },
      {
        q: '¿Puedo cancelar cuando quiera?',
        a: 'Sí. Puedes cancelar desde tu cuenta en cualquier momento; el plan sigue hasta el fin del periodo pagado.',
      },
    ],
  },
  finalCta: {
    title: '¿Listo para empezar?',
    body: 'Crea tu cuenta y prueba @@APP_NAME@@ gratis durante 14 días.',
    primary: 'Empezar prueba gratis',
    secondary: 'Por qué appgrove',
  },
}
