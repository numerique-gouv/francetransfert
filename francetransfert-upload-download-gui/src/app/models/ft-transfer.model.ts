/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

export class FTTransferModel<T> {
    id: string;
    name: string;
    flowFile: flowjs.FlowFile;
    progress: number;
    error: boolean;
    paused: boolean;
    success: boolean;
    complete: boolean;
    currentSpeed: number;
    averageSpeed: number;
    size: number;
    timeRemaining: number;
    folder?: boolean;
    childs?: Array<T>;
    constructor(name: string, size: number, file: T) {
        this.id = `${Math.floor(Math.random() * Math.floor(100))}-${name}`;
        this.name = name;
        this.progress = 0;
        this.currentSpeed = 0;
        this.averageSpeed = 0;
        this.error = false;
        this.paused = false;
        this.success = false;
        this.size = size;
        this.timeRemaining = 0;
        this.childs = [file];
        this.folder = true;
    }
}
