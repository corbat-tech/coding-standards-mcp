/**
 * Article Entity - Domain layer
 * Represents the core business entity for articles
 */
export interface ArticleProps {
  id?: string;
  title: string;
  content: string;
  authorId: string;
  tags?: string[];
  createdAt?: Date;
  updatedAt?: Date;
}

export class Article {
  private readonly _id: string;
  private _title: string;
  private _content: string;
  private readonly _authorId: string;
  private _tags: string[];
  private readonly _createdAt: Date;
  private _updatedAt: Date;

  constructor(props: ArticleProps) {
    this._id = props.id ?? this.generateId();
    this._title = props.title;
    this._content = props.content;
    this._authorId = props.authorId;
    this._tags = props.tags ?? [];
    this._createdAt = props.createdAt ?? new Date();
    this._updatedAt = props.updatedAt ?? new Date();
  }

  get id(): string {
    return this._id;
  }

  get title(): string {
    return this._title;
  }

  get content(): string {
    return this._content;
  }

  get authorId(): string {
    return this._authorId;
  }

  get tags(): string[] {
    return [...this._tags];
  }

  get createdAt(): Date {
    return this._createdAt;
  }

  get updatedAt(): Date {
    return this._updatedAt;
  }

  update(title: string, content: string, tags?: string[]): void {
    this._title = title;
    this._content = content;
    if (tags !== undefined) {
      this._tags = tags;
    }
    this._updatedAt = new Date();
  }

  private generateId(): string {
    return `article_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  toJSON(): ArticleProps {
    return {
      id: this._id,
      title: this._title,
      content: this._content,
      authorId: this._authorId,
      tags: [...this._tags],
      createdAt: this._createdAt,
      updatedAt: this._updatedAt,
    };
  }
}
