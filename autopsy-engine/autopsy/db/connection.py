"""Database connection pool management."""

from __future__ import annotations

import os
from contextlib import contextmanager
from typing import TYPE_CHECKING, Any

from psycopg_pool import ConnectionPool

if TYPE_CHECKING:
    from collections.abc import Generator

    from psycopg import Connection

_pool: ConnectionPool | None = None


def get_pool() -> ConnectionPool:
    """Get or create the connection pool."""
    global _pool  # noqa: PLW0603
    if _pool is None:
        conninfo = os.environ.get(
            "DATABASE_URL",
            "postgresql://autopsy:autopsy@localhost:5432/autopsy",
        )
        _pool = ConnectionPool(conninfo=conninfo, min_size=2, max_size=10)
    return _pool


@contextmanager
def get_connection() -> Generator[Connection[Any], None, None]:
    """Get a connection from the pool as a context manager."""
    pool = get_pool()
    with pool.connection() as conn:
        yield conn


def close_pool() -> None:
    """Close the connection pool."""
    global _pool  # noqa: PLW0603
    if _pool is not None:
        _pool.close()
        _pool = None
