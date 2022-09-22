import {Request} from "./request.model";

export class ActiveProcessRequest extends Request {
  static path: string = '/active';

  constructor() {
    super(ActiveProcessRequest.path);
  }

  getFormData(): FormData {
    return new FormData();
  }

}
