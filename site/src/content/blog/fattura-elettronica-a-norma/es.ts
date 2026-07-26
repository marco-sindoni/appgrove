// Articolo cluster "Fattura elettronica a norma" — spagnolo (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const es: PostLocaleContent = {
  slug: 'factura-electronica-conforme',
  title: 'Cómo crear una factura electrónica conforme',
  description:
    'Una respuesta paso a paso: qué hace conforme a una factura electrónica en la UE, los campos que debe llevar y cómo enviarla y conservarla para que resista una inspección.',
  question: '¿Cómo creo una factura electrónica conforme?',
  intro: [
    'Una factura electrónica no es simplemente un PDF que envías por correo. En el sentido de la UE, es una factura emitida, transmitida y recibida en un formato electrónico estructurado que un ordenador puede procesar automáticamente — y ser conforme significa que lleva los campos correctos y se conserva de forma que no pueda alterarse en silencio.',
    'Esta es la versión breve y práctica: qué poner en ella, cómo enviarla y cómo conservarla. Nada difícil una vez que lo haces siempre igual — que es exactamente para lo que sirve una herramienta.',
  ],
  sections: [
    {
      heading: 'Los campos que debe llevar',
      paragraphs: [
        'Parte del núcleo obligatorio: tus datos y los de tu cliente, un número único y correlativo, la fecha de emisión, una descripción clara línea por línea, la base imponible, el tipo y el importe de IVA (o el motivo de la exención) y el total. Para clientes empresa, en muchos países, hace falta también su número de identificación fiscal; para las ventas transfronterizas entre empresas, la indicación de que se aplica la inversión del sujeto pasivo.',
        'Si olvidas uno, la factura puede rechazarse o cuestionarse. El valor de rellenarlos a partir de datos estructurados — cliente, producto, regla fiscal — en lugar de reescribirlos es que los mismos campos salen correctos cada vez.',
      ],
    },
    {
      heading: 'Enviarla en el formato correcto',
      paragraphs: [
        'La conformidad significa cada vez más un formato estructurado, no la imagen de una factura. La facturación a la administración pública en la UE ya usa formatos electrónicos estructurados, y varios países encaminan las facturas entre empresas a través de una plataforma nacional o un sistema de control antes de que lleguen al cliente.',
        'La conclusión práctica: comprueba si tu país o tu cliente exigen un canal específico y genera la factura en un formato que ese canal acepte. Una herramienta que produce el formato estructurado por ti convierte una cuestión de cumplimiento en un no-evento.',
      ],
    },
    {
      heading: 'Conservarla para que resista',
      paragraphs: [
        'Una factura electrónica conforme debe conservarse durante todo el periodo de conservación — a menudo diez años — de forma que se preserven su autenticidad e integridad. En términos claros: debes poder demostrar que no se ha modificado desde su emisión y presentarla cuando se solicite.',
        'Es tanto una decisión de conservación como de formato. Con appgrove tus facturas se archivan en la UE, bajo la ley europea, con plenos derechos RGPD — de modo que el documento en el que te apoyarás dentro de años sigue siendo tuyo y bajo tu control.',
      ],
    },
  ],
  faq: {
    title: 'Preguntas sobre facturas electrónicas conformes',
    items: [
      {
        q: '¿Un PDF es una factura electrónica?',
        a: 'No en el sentido estricto de la UE. Un PDF es una imagen legible por personas; una factura electrónica conforme se emite en un formato estructurado que un ordenador puede procesar automáticamente. Donde se exige el formato estructurado, un simple PDF no basta.',
      },
      {
        q: '¿Cuál es el error más común?',
        a: 'Un salto en la numeración correlativa, o un campo obligatorio ausente como el tratamiento del IVA en una venta transfronteriza. Ambos se evitan cuando la factura se construye a partir de datos estructurados en lugar de escribirse a mano cada vez.',
      },
      {
        q: '¿Cómo demuestro que una factura no se ha alterado?',
        a: 'Conservándola en un sistema que preserve su integridad durante todo el periodo de conservación y pueda presentarla sin cambios cuando se solicite. La clave no es una técnica única, sino un sistema de archivo que puedas defender en una inspección.',
      },
    ],
  },
  ctaText: 'Crea facturas conformes con appgrove Facturas',
}
