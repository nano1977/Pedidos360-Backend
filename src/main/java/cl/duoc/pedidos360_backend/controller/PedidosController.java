package cl.duoc.pedidos360_backend.controller;



import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosController {

    @GetMapping
    public Map<String, Object> obtenerPedidos(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "status", "success",
            "mensaje", "Acceso concedido al BFF de Pedidos360",
            "usuario", jwt.getClaimAsString("name"),
            "tenant", jwt.getClaimAsString("tid")
        );
    }
}
