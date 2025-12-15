"""add friend requests

Revision ID: b1f5d9c9b2e2
Revises: 4c6748c8a871
Create Date: 2025-12-15 12:40:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'b1f5d9c9b2e2'
down_revision = '4c6748c8a871'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table('friend_requests',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('from_uid', sa.String(length=128), nullable=False),
        sa.Column('to_uid', sa.String(length=128), nullable=False),
        sa.Column('status', sa.String(length=32), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=True),
        sa.Column('responded_at', sa.DateTime(), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )


def downgrade():
    op.drop_table('friend_requests')
