import { Injectable } from '@angular/core';

export interface PreparedImageResult {
  file: File;
  optimized: boolean;
  originalSize: number;
}

@Injectable({ providedIn: 'root' })
export class ImageUploadService {
  private readonly targetMaxBytes = 4.5 * 1024 * 1024;
  private readonly optimizationThresholdBytes = 1.2 * 1024 * 1024;
  private readonly maxDimension = 1600;

  async prepareForUpload(file: File): Promise<PreparedImageResult> {
    if (!file || !file.type.startsWith('image/')) {
      throw new Error('Seleccione una imagen válida.');
    }

    if (this.isAcceptedByBackend(file) && file.size <= this.optimizationThresholdBytes) {
      return { file, optimized: false, originalSize: file.size };
    }

    const image = await this.loadImage(file).catch(() => null);
    if (!image) {
      throw new Error(
        'No fue posible procesar la foto seleccionada. Usa una imagen JPG o PNG desde la galería.'
      );
    }

    const { width, height } = this.getScaledDimensions(image.width, image.height);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;

    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('No fue posible preparar la imagen para el envío.');
    }

    context.drawImage(image, 0, 0, width, height);

    let blob: Blob | null = null;
    for (const quality of [0.9, 0.82, 0.74, 0.66, 0.58]) {
      blob = await this.canvasToBlob(canvas, 'image/jpeg', quality);
      if (blob && blob.size <= this.targetMaxBytes) break;
    }

    if (!blob) {
      throw new Error('No fue posible convertir la imagen seleccionada.');
    }

    const converted = new File([blob], this.normalizeName(file.name), {
      type: 'image/jpeg',
      lastModified: Date.now(),
    });

    if (converted.size > this.targetMaxBytes) {
      throw new Error('La foto sigue siendo muy pesada. Intenta con una imagen más ligera.');
    }

    return {
      file: converted,
      optimized: true,
      originalSize: file.size,
    };
  }

  private isAcceptedByBackend(file: File): boolean {
    const name = file.name.toLowerCase();
    return (
      (file.type === 'image/jpeg' || file.type === 'image/png') &&
      (name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png'))
    );
  }

  private normalizeName(name: string): string {
    const cleanName = name.replace(/\.[^.]+$/, '').replace(/[^a-zA-Z0-9_-]/g, '_');
    return `${cleanName || 'imagen'}_movil.jpg`;
  }

  private getScaledDimensions(width: number, height: number): { width: number; height: number } {
    if (width <= this.maxDimension && height <= this.maxDimension) {
      return { width, height };
    }

    const ratio = Math.min(this.maxDimension / width, this.maxDimension / height);
    return {
      width: Math.max(1, Math.round(width * ratio)),
      height: Math.max(1, Math.round(height * ratio)),
    };
  }

  private loadImage(file: File): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const url = URL.createObjectURL(file);
      const image = new Image();
      image.onload = () => {
        URL.revokeObjectURL(url);
        resolve(image);
      };
      image.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('No fue posible cargar la imagen.'));
      };
      image.src = url;
    });
  }

  private canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob | null> {
    return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob), type, quality));
  }
}
