// Pilastro "Fatturazione per piccole imprese in UE" — spagnolo (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const es: PostLocaleContent = {
  slug: 'facturacion-pequenas-empresas-ue',
  title: 'La facturación para pequeñas empresas en la UE',
  description:
    'Una guía en lenguaje claro sobre la facturación para pequeñas empresas en la UE: qué debe llevar una factura conforme, cómo funcionan el IVA y las ventas transfronterizas, y cómo conservar los registros respetando el RGPD.',
  question: '¿Cómo funciona la facturación para una pequeña empresa en la UE?',
  intro: [
    'Si llevas una pequeña empresa en Europa, la facturación es donde el papeleo se acumula en silencio: cada venta necesita un documento correcto, numerado, conservado durante años y —cada vez más— electrónico. Un error significa arriesgarse a una factura rechazada, un pago tardío o una pregunta de Hacienda sin respuesta.',
    'Esta guía es el mapa. Cubre qué debe contener una factura conforme, cómo cambian las cosas con el impuesto sobre el valor añadido (IVA) y con las ventas transfronterizas, y cómo conservar los registros sin convertir tus datos en el activo de otro. Los artículos enlazados profundizan en las dos preguntas más frecuentes.',
  ],
  sections: [
    {
      heading: 'Qué debe contener una factura conforme',
      paragraphs: [
        'En toda la UE una factura comparte un núcleo común: quién vende y quién compra, un número único y correlativo, la fecha, una descripción clara de lo vendido, los importes con y sin impuesto y el tipo de IVA aplicado (o el motivo de su ausencia). Las normas nacionales añaden detalles, pero ese esqueleto es el mismo tanto si facturas en Milán, Madrid o Múnich.',
        'El número importa más de lo que parece: debe ser único y sin saltos dentro de tu serie de numeración, porque es la forma en que Hacienda rastrea el documento. Una herramienta que lo asigna por ti elimina la fuente de error manual más común.',
      ],
    },
    {
      heading: 'IVA y ventas transfronterizas',
      paragraphs: [
        'El impuesto sobre el valor añadido es la parte que hace tropezar. Vender a una empresa de otro país de la UE a menudo significa que es el comprador quien liquida el impuesto (mecanismo de inversión del sujeto pasivo): tu factura no muestra IVA pero debe indicar por qué. Vender a consumidores más allá de las fronteras puede llevarte al régimen de ventanilla única (One-Stop-Shop) una vez superado un umbral. Las reglas se aprenden, pero no perdonan las aproximaciones.',
        'El hábito seguro es decidir el tratamiento del IVA antes de enviar, no después. Conocer el estatus y el país de tu cliente, y anotarlo en la factura, es lo que mantiene limpia una venta transfronteriza ante una inspección.',
      ],
    },
    {
      heading: 'Conservar los registros — y que sigan siendo tuyos',
      paragraphs: [
        'La mayoría de los países de la UE obligan a conservar las facturas durante varios años (a menudo diez), y muchos ya imponen o fomentan la factura electrónica en un formato estructurado. Eso significa que tu archivo no es un cajón de papel: son datos, y dónde viven esos datos es una decisión real.',
        'Aquí appgrove toma una postura clara: tus datos de facturación se alojan en la UE, bajo la ley europea, con plenos derechos RGPD y sin rastreadores ocultos. El cumplimiento no es un añadido que incorporas después: es el contexto por defecto en el que están tus documentos desde el primer día.',
      ],
    },
  ],
  faq: {
    title: 'Preguntas frecuentes sobre la facturación en la UE',
    items: [
      {
        q: '¿Estoy obligado a emitir facturas electrónicas en la UE?',
        a: 'Depende del país y del cliente. La facturación a la administración pública ya es electrónica en toda la UE, y varios países están extendiendo la factura electrónica estructurada a las ventas entre empresas. Incluso donde el papel sigue permitido, los registros electrónicos son más fáciles de conservar y de demostrar.',
      },
      {
        q: '¿Cuánto tiempo debo conservar mis facturas?',
        a: 'La mayoría de los Estados miembros de la UE exigen conservar las facturas durante varios años — a menudo diez. El plazo exacto se fija a nivel nacional, así que comprueba la norma de tu país, pero prevé un almacenamiento a largo plazo y a prueba de manipulaciones, no archivos sueltos.',
      },
      {
        q: '¿Qué pasa si mi numeración de facturas tiene un salto?',
        a: 'Un salto o un duplicado en tu numeración correlativa es una señal de alarma en una inspección, porque la secuencia es la forma en que Hacienda comprueba que no falta ninguna factura. La numeración automática y sin saltos es la manera más sencilla de evitar el problema por completo.',
      },
    ],
  },
  ctaText: 'Descubre cómo appgrove Facturas se encarga por ti',
}
