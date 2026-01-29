export class Article {
  readonly id: string;
  readonly title: string;
  readonly content: string;
  readonly author: string;
  readonly tags: string[];
  readonly published: boolean;
  readonly createdAt: Date;
  readonly updatedAt: Date;

  constructor(props: {
    id: string;
    title: string;
    content: string;
    author: string;
    tags?: string[];
    published?: boolean;
    createdAt?: Date;
    updatedAt?: Date;
  }) {
    this.id = props.id;
    this.title = props.title;
    this.content = props.content;
    this.author = props.author;
    this.tags = props.tags ?? [];
    this.published = props.published ?? false;
    this.createdAt = props.createdAt ?? new Date();
    this.updatedAt = props.updatedAt ?? new Date();
  }

  update(props: Partial<Omit<Article, 'id' | 'createdAt'>>): Article {
    return new Article({
      id: this.id,
      title: props.title ?? this.title,
      content: props.content ?? this.content,
      author: props.author ?? this.author,
      tags: props.tags ?? this.tags,
      published: props.published ?? this.published,
      createdAt: this.createdAt,
      updatedAt: new Date(),
    });
  }

  publish(): Article {
    return this.update({ published: true });
  }

  unpublish(): Article {
    return this.update({ published: false });
  }
}
