import {Dataset} from "./dataset.model";
import {Filter} from "./filter.model";

export class Dictionary {
  count: number;
  datasets: Dataset[];
  filters: { [key: string]: Filter[] };

  constructor(obj?: any) {
    this.count = obj && obj.count || null;
    this.datasets = obj && obj.datasets || null;
    this.filters = obj && obj.filters || null;
  }
}
