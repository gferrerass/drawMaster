"""remove avatar_url

Revision ID: c3d9a1b2e4f6
Revises: b1f5d9c9b2e2
Create Date: 2025-12-17 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'c3d9a1b2e4f6'
down_revision = 'b1f5d9c9b2e2'
branch_labels = None
depends_on = None


def upgrade():
    # drop the avatar_url column from user_profiles
    with op.batch_alter_table('user_profiles') as batch_op:
        batch_op.drop_column('avatar_url')


def downgrade():
    # add avatar_url column back
    with op.batch_alter_table('user_profiles') as batch_op:
        batch_op.add_column(sa.Column('avatar_url', sa.String(length=512), nullable=True))
