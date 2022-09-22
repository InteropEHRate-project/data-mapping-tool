export abstract class Request {
  path: string;

  protected constructor(path: string) {
    this.path=path;
  }

  abstract getFormData(): FormData;
}
