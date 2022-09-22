export interface Process {
  dataInputStream: boolean;
  processName: string;
  fileUrl: string;
  rawData: string;
  r2rmlURL: string;
  contentType: string;
  maxNumLines: number;
  columnDelimiter: string;
  headerStartIndex: number;
  dataStartIndex: number;
  encoding: string;
  emlURL: string;
  rdfURL: string;
  outputSourceType: string;
  outputDataFormat: string;
  emlOutputStream: string;
  rdfOutputStream: string;
  completed: boolean;
  error: boolean;
  message: string;
  alive: boolean;
  scheduled: boolean;
}
