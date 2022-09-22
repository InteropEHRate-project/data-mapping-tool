import {Request} from "./request.model";
import {ActionEnum} from "../api.enum";

export class TopTagsRequest extends Request {
  facet_limit: number;

  constructor(obj?: any) {
    super(
      ActionEnum.package_search,
      null,
      null,
      0
    );
    this.facet_limit = obj && obj.facet_limit || null;
  }

  public getQueryParameters(): string {
    let params: Array<string> = [];
    params.push(`facet.field=["tags"]`);
    if (this.facet_limit) params.push(`facet.limit=${this.facet_limit}`);
    params.push(`rows=${this.row.toString()}`);
    return params.join('&');
  }

}
