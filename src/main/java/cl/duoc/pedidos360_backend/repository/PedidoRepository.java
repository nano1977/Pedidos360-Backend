package cl.duoc.pedidos360_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pedidos360_backend.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}