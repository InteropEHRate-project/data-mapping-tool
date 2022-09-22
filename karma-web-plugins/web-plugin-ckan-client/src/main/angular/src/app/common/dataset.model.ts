import {Resource} from "./resource.model";

export class Dataset {
  id: string;
  resources: Array<Resource>;
  title: string;
  notes: string;
  license_title: string;
  url: string;
  author: string;
  type: string;
  isShowMore: boolean;

  constructor(obj?: any) {
    this.id = obj && obj.id;
    this.title = obj && obj.title;
    this.resources = obj && obj.resources || null;
    this.notes = obj && obj.notes || null;
    this.url = obj && obj.url || null;
    this.license_title = obj && obj.license_title || null;
    this.author = obj && obj.author || null;
    this.type = obj && obj.type || null;
    this.isShowMore = false;
  }

  getResourceIds(): Array<string> {
    let list: Array<string> = new Array<string>();
    if (this.resources) {
      this.resources.forEach(r => {
        if (r.selectedForImport) list.push(r.id);
      })
    }
    return list;
  }

  static deserialize(item: any): Dataset {
    return new Dataset({
      id: item.id,
      title: item.title,
      notes: item.notes,
      url: item.url,
      license_title: item.license_title,
      author: item.author,
      type: item.type,
      resources: item.resources.map(resource => {
        return Resource.deserialize(resource);
      })
    })
  };

}
