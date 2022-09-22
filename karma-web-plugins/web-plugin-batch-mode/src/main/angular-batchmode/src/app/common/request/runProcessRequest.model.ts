import {Request} from "./request.model";
import {FormParameterEnum} from "../api.enum";

export class RunProcessRequest extends Request {
  static path: string = '/run';
  datasetUrl: string;
  dataFile: string;
  r2rmlUrl: string;
  r2rmlFile: string;
  contentType: string;
  outputDataFormat: string;


  constructor(obj?: any) {
    super(RunProcessRequest.path);
    this.datasetUrl = obj && obj.datasetUrl || null;
    this.dataFile = obj && obj.dataFile || null;
    this.r2rmlUrl = obj && obj.r2rmlUrl || null;
    this.r2rmlFile = obj && obj.r2rmlFile || null;
    this.contentType = obj && obj.contentType || null;
    this.outputDataFormat = obj && obj.outputDataFormat || null;
  }

  public getFormData(): FormData {
    let formData = new FormData();
    if (this.datasetUrl) formData.append(FormParameterEnum.datasetUrl, this.datasetUrl);
    if (this.dataFile) formData.append(FormParameterEnum.dataFile, this.dataFile);
    if (this.r2rmlUrl) formData.append(FormParameterEnum.r2rmlUrl, this.r2rmlUrl);
    if (this.r2rmlFile) formData.append(FormParameterEnum.r2rmlFile, this.r2rmlFile);
    if (this.contentType) formData.append(FormParameterEnum.contentType, this.contentType);
    if (this.outputDataFormat) formData.append(FormParameterEnum.outputDataFormat, this.outputDataFormat);
    return formData;
  }

}
