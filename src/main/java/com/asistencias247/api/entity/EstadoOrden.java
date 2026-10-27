package com.asistencias247.api.entity;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoOrden {
    PENDIENTE {
        @Override
        public Set<EstadoOrden> siguientesPermitidos() {
            return EnumSet.of(EN_CAMINO, EN_DOMICILIO, FINALIZADA, CANCELADA);
        }
    },
    EN_CAMINO {
        @Override
        public Set<EstadoOrden> siguientesPermitidos() {
            return EnumSet.of(EN_DOMICILIO, FINALIZADA, CANCELADA);
        }
    },
    EN_DOMICILIO {
        @Override
        public Set<EstadoOrden> siguientesPermitidos() {
            return EnumSet.of(FINALIZADA, CANCELADA);
        }
    },
    FINALIZADA {
        @Override
        public Set<EstadoOrden> siguientesPermitidos() {
            return EnumSet.noneOf(EstadoOrden.class);
        }
    },
    CANCELADA {
        @Override
        public Set<EstadoOrden> siguientesPermitidos() {
            return EnumSet.of(CANCELADA, PENDIENTE);
        }
    };

    public abstract Set<EstadoOrden> siguientesPermitidos();

    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino != null && siguientesPermitidos().contains(destino);
    }

    public boolean esCancelable() {
        return puedeTransicionarA(CANCELADA);
    }
}
