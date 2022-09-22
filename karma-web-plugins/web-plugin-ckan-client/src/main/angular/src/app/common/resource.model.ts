export class Resource {
  id: string
  resource_type: string;
  license_type: string;
  url: string;
  last_modified: string;
  created: string;
  name: string;
  format: string;
  description: string;
  state: string;
  size: number;
  distribution_format: string;
  selectedForImport: boolean;

  constructor(obj?: any) {
    this.id = obj && obj.id;
    this.resource_type = obj && obj.resource_type || null;
    this.license_type = obj && obj.license_type || null;
    this.last_modified = obj && obj.last_modified || null;
    this.created = obj && obj.created || null;
    this.name = obj && obj.name || null;
    this.format = obj && obj.format || null;
    this.url = obj && obj.url || null;
    this.description = obj && obj.description || null;
    this.state = obj && obj.state || null;
    this.size = obj && obj.size || null;
    this.distribution_format = obj && obj.distribution_format || null;
    this.selectedForImport = true;
  }

  static deserialize(resource: any): Resource {
    return new Resource({
      id: resource.id,
      resource_type: resource.resource_type,
      license_type: resource.license_type,
      last_modified: resource.last_modified,
      created: resource.created,
      name: resource.name,
      url: resource.url,
      format: resource.format,
      description: resource.description,
      state: parseInt(resource.state, 10),
      size: resource.size,
      distribution_format: resource.distribution_format
    });
  }
}
