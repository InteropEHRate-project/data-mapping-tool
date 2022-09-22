import {Request} from "./request.model";

export class HistoryRequest extends Request {
  static path: string = '/history';

  constructor() {
    super(HistoryRequest.path);
  }

  getFormData(): FormData {
    return new FormData();
  }

}
