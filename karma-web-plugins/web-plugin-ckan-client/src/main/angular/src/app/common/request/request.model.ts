import {ActionEnum} from "../api.enum";

export abstract class Request {
  action: ActionEnum;
  resultHeader: string;
  start: number;
  row: number;

  protected constructor(action: ActionEnum, resultHeader: string, start: number,
                        row: number) {
    this.action = action;
    this.resultHeader = resultHeader;
    this.start = start;
    this.row = row;
  }

  abstract getQueryParameters(): string;
}
