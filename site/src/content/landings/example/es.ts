// Fixture di esempio (UC 0038) — spagnolo.
import type { LandingLocaleContent } from '../types.ts'

export const es: LandingLocaleContent = {
  slug: 'ejemplo',
  meta: {
    title: 'Ejemplo — la plantilla de landing de appgrove',
    description:
      'Demostración de la plantilla repetible de landing por aplicación: ocho secciones de marca, cinco idiomas, estados borrador y publicado.',
    ogImage: null,
  },
  hero: {
    badge: 'todo en UE · RGPD primero',
    title: 'Termina el trabajo en minutos, no en tardes enteras',
    subtitle:
      'Ejemplo es la aplicación de demostración de la plantilla de landing de appgrove. Muestra cómo cada app cuenta su historia — primero el trabajo, la privacidad como firma de confianza.',
    ctaPrimary: 'Empezar prueba gratuita',
    ctaSecondary: 'Ver cómo funciona',
    screenshot: {
      src: null,
      alt: 'Captura de pantalla del panel de la aplicación Ejemplo',
    },
  },
  problemSolution: {
    title: 'Menos administración, más de lo que importa',
    problem:
      'Los equipos pequeños pierden horas en tareas repetitivas y tediosas — repartidas entre herramientas que nunca se comunican del todo.',
    solution:
      'Ejemplo hace bien una sola cosa: te quita esa administración de encima, en pocos clics, con tus datos a salvo en Europa.',
  },
  features: {
    title: 'Todo lo que necesitas, nada de sobra',
    subtitle: 'Funciones centradas que hacen bien un trabajo — sin suites infladas, sin ataduras.',
    items: [
      {
        icon: 'bolt',
        title: 'Rápida por defecto',
        body: 'Puesta en marcha en minutos y tareas diarias en un par de clics.',
      },
      {
        icon: 'lock',
        title: 'Privada por diseño',
        body: 'Tus datos viven en la UE, bajo ley europea, con plenos derechos RGPD.',
      },
      {
        icon: 'sync',
        title: 'Una cuenta, todas las herramientas',
        body: 'Añade otras apps de appgrove cuando las necesites — mismo acceso, mismo hogar de confianza.',
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
      { title: 'Crea tu cuenta', body: 'Registro en segundos — sin tarjeta para empezar la prueba.' },
      { title: 'Configura tu espacio', body: 'Una configuración guiada te hace productivo desde el primer día.' },
      { title: 'Termina el trabajo', body: 'Haz tu trabajo y deja que Ejemplo mantenga la administración a raya.' },
    ],
  },
  pricing: {
    title: 'Precios simples y justos',
    subtitle: 'Elige el plan adecuado. El anual cuesta menos; el mensual da flexibilidad.',
    monthlyLabel: 'Mensual',
    yearlyLabel: 'Anual',
    trialNote: 'Cada plan de pago empieza con 14 días de prueba gratuita — no se cobra nada hasta que termina.',
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
    title: 'Alojada en la UE. Plenos derechos RGPD.',
    body: 'Aquí la privacidad no es un extra — es cómo está construido appgrove. Tus datos se quedan en Europa y nunca se venden.',
    points: ['Todos los datos alojados en la UE', 'Plenos derechos RGPD, por diseño', 'Sin rastreadores ocultos, ningún dato vendido'],
  },
  faq: {
    title: 'Preguntas frecuentes',
    items: [
      {
        q: '¿Necesito una tarjeta para probar Ejemplo?',
        a: 'No. La prueba gratuita de 14 días empieza sin tarjeta; solo pagas si decides continuar.',
      },
      {
        q: '¿Dónde se guardan mis datos?',
        a: 'Íntegramente en la Unión Europea, bajo ley europea, con plenos derechos RGPD.',
      },
      {
        q: '¿Puedo cancelar cuando quiera?',
        a: 'Sí. Puedes cancelar desde tu cuenta en cualquier momento; el plan sigue hasta el final del periodo pagado.',
      },
    ],
  },
  finalCta: {
    title: '¿Listo para empezar?',
    body: 'Crea tu cuenta y prueba Ejemplo gratis durante 14 días.',
    primary: 'Empezar prueba gratuita',
    secondary: 'Por qué appgrove',
  },
}
