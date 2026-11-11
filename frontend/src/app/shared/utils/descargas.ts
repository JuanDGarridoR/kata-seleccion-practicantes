/**
 * Las descargas protegidas no pueden ser un simple <a href>: el navegador no enviaría
 * el header Authorization. Se piden con HttpClient como Blob y se guardan con un enlace temporal.
 */
export function guardarArchivo(contenido: Blob, nombre: string): void {
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
}
