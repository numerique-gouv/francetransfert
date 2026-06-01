import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { v4 as uuidv4 } from "uuid";
import { LoginService } from 'src/app/services/login/login.service';

@Injectable()
export class CorrelationIdInterceptor implements HttpInterceptor {
    private readonly correlationIdHeader = 'x-correlation-id';
    private readonly sessionIdHeader = 'x-session-id';
    constructor(private loginService: LoginService) { }
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        let requestToHandle = req;

        if (!requestToHandle.headers.has(this.correlationIdHeader)) {
            const correlationId = uuidv4();
            requestToHandle = requestToHandle.clone({
                headers: requestToHandle.headers.set(this.correlationIdHeader, correlationId),
            });
        }

        if (!requestToHandle.headers.has(this.sessionIdHeader)) {
            const sessionId = this.loginService.sessionId;
            requestToHandle = requestToHandle.clone({
                headers: requestToHandle.headers.set(this.sessionIdHeader, sessionId),
            });
        }

        return next.handle(requestToHandle);
    }
}
