import {FiltersEnum} from "./api.enum";

export class Filter {
  type: FiltersEnum
  name: string;
  display_name: string;
  count: number;
  activated: boolean;

  constructor(obj?: any) {
    this.type = obj && obj.type || null;
    this.name = obj && obj.name || null;
    this.display_name = obj && obj.display_name || null;
    this.count = obj && obj.count || null;
    this.activated = obj && obj.activated || false;
  }

  toString(): string {
    return `"${this.name.replace(/\s+/g, '%20')}"`;
  }

  isEqual(obj: Filter): boolean {
    return this.type === obj.type
      && this.name === obj.name
      && this.display_name === obj.display_name;
  }

  static deserialize(type: string, item: any): Filter {
    return new Filter({
      type: FiltersEnum[type],
      name: item.name,
      display_name: item.display_name,
      count: item.count
    });
  }
}
