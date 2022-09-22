import {Request} from "./request.model";
import {ActionEnum, FiltersEnum} from "../api.enum";
import {Filter} from "../filter.model";

export class DatasetsSearchRequest extends Request {
  selectedFilters: Filter[];
  freeTextSearch: string;

  constructor(obj?: any) {
    super(
      ActionEnum.package_search,
      'Results',
      obj && obj.start || null,
      obj && obj.row || null
    );
    this.selectedFilters = obj && obj.selectedFilters || null;
    this.freeTextSearch = obj && obj.freeTextSearch || null;

  }

  public getQueryParameters(): string {
    let params: Array<string> = [];
    params.push(`facet.field=${JSON.stringify(Object.values(FiltersEnum))}`);
    if (this.start) params.push(`start=${this.start.toString()}`);
    if (this.row) params.push(`rows=${this.row.toString()}`);
    if (this.selectedFilters && this.selectedFilters.length)
      params.push(`fq=(${DatasetsSearchRequest.getSelectedFilters(this.selectedFilters)})`);
    if (this.freeTextSearch) params.push(`q=${this.freeTextSearch}`);
    return params.join('&');
  }


  private static getSelectedFilters(selectedFilters: Filter[]): string {
    let fq: Array<string> = [];

    let tags: Array<string> = [];
    let tagsString: string = null;

    let organization: Array<string> = [];
    let organizationString: string = null;

    let groups: Array<string> = [];
    let groupsString: string = null;

    let res_format: Array<string> = [];
    let res_formatString: string = null;

    let resource_license_en: Array<string> = [];
    let resource_license_enString: string = null;

    selectedFilters.forEach(filter => {
      if (filter.type === FiltersEnum.tags) tags.push(filter.toString());
      if (filter.type === FiltersEnum.organization) organization.push(filter.toString());
      if (filter.type === FiltersEnum.groups) groups.push(filter.toString());
      if (filter.type === FiltersEnum.res_format) res_format.push(filter.toString());
      if (filter.type === FiltersEnum.resource_license_en) resource_license_en.push(filter.toString());
    });
    tagsString = DatasetsSearchRequest.array2String(tags);
    organizationString = DatasetsSearchRequest.array2String(organization);
    groupsString = DatasetsSearchRequest.array2String(groups);
    res_formatString = DatasetsSearchRequest.array2String(res_format);
    resource_license_enString = DatasetsSearchRequest.array2String(resource_license_en);

    if (tagsString) fq.push(`${FiltersEnum.tags}:${tagsString}`);
    if (organizationString) fq.push(`${FiltersEnum.organization}:${organizationString}`);
    if (groupsString) fq.push(`${FiltersEnum.groups}:${groupsString}`);
    if (res_formatString) fq.push(`${FiltersEnum.res_format}:${res_formatString}`);
    if (resource_license_enString) fq.push(`${FiltersEnum.resource_license_en}:${resource_license_enString}`);

    return fq.join(`"AND"`);
  }

  private static array2String(filters: Array<string>): string {
    return filters.length == 1 ? filters.join() : filters.length > 1 ? `(${filters.join(`"AND"`)})` : null;
  }
}
